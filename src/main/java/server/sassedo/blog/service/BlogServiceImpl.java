package server.sassedo.blog.service;

import lombok.RequiredArgsConstructor;
import org.owasp.html.CssSchema;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import server.sassedo.blog.data.dto.BlogImage;
import server.sassedo.blog.data.dto.BlogPost;
import server.sassedo.blog.data.network.request.BlogPostRequest;
import server.sassedo.blog.repository.BlogImageRepository;
import server.sassedo.blog.repository.BlogPostRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.utils.ImageUploadValidator;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogPostRepository blogPostRepository;
    private final BlogImageRepository blogImageRepository;

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTI_HYPHEN = Pattern.compile("-{2,}");

    // Allow rich formatting from the admin WYSIWYG editor while stripping scripts/handlers (stored-XSS defense).
    private static final PolicyFactory HTML_POLICY = new HtmlPolicyBuilder()
            .allowStandardUrlProtocols()
            .allowElements(
                    "p", "br", "span", "div", "b", "strong", "i", "em", "u", "s", "strike",
                    "sub", "sup", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "code",
                    "ul", "ol", "li", "a", "img", "hr", "figure", "figcaption")
            .allowAttributes("href", "title", "target").onElements("a")
            .allowAttributes("src", "alt", "title", "width", "height").onElements("img")
            .allowStyling(CssSchema.DEFAULT)
            .requireRelNofollowOnLinks()
            .toFactory();

    @Override
    public Page<BlogPost> getPublished(Pageable pageable) {
        return blogPostRepository.findByPublishedTrue(pageable);
    }

    @Override
    public Page<BlogPost> adminGetAll(Pageable pageable) {
        return blogPostRepository.findAll(pageable);
    }

    @Override
    public BlogPost getPublishedBySlug(String slug) throws GenericException {
        BlogPost post = blogPostRepository.findBySlug(slug)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.BLOG_POST_NOT_FOUND, "Blog post not found"));
        if (!post.isPublished()) {
            throw new GenericException(GenericExceptionCode.BLOG_POST_NOT_FOUND, "Blog post not found");
        }
        return post;
    }

    @Override
    public BlogPost getById(Long id) throws GenericException {
        return blogPostRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.BLOG_POST_NOT_FOUND, "Blog post not found"));
    }

    @Override
    @Transactional
    public BlogPost create(Long authorId, BlogPostRequest request) throws GenericException {
        BlogPost post = new BlogPost();
        post.setAuthorId(authorId);
        applyRequest(post, request, null);
        return blogPostRepository.save(post);
    }

    @Override
    @Transactional
    public BlogPost update(Long id, BlogPostRequest request) throws GenericException {
        BlogPost post = getById(id);
        applyRequest(post, request, post.getId());
        return blogPostRepository.save(post);
    }

    @Override
    @Transactional
    public void delete(Long id) throws GenericException {
        BlogPost post = getById(id);
        blogPostRepository.delete(post);
    }

    @Override
    @Transactional
    public BlogImage addImage(MultipartFile file) throws GenericException, IOException {
        if (file == null || file.isEmpty()) {
            throw new GenericException(GenericExceptionCode.INVALID_FILE, "Uploaded file is empty");
        }
        ImageUploadValidator.validate(file);
        BlogImage image = new BlogImage();
        image.setData(file.getBytes());
        image.setContentType(file.getContentType());
        return blogImageRepository.save(image);
    }

    @Override
    public BlogImage getImage(Long id) throws GenericException {
        return blogImageRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.BLOG_IMAGE_NOT_FOUND, "Blog image not found"));
    }

    private void applyRequest(BlogPost post, BlogPostRequest request, Long currentId) throws GenericException {
        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }

        String desiredSlug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? request.getSlug()
                : (post.getSlug() != null ? post.getSlug() : request.getTitle());
        post.setSlug(ensureUniqueSlug(slugify(desiredSlug), currentId));

        if (request.getExcerpt() != null) {
            post.setExcerpt(request.getExcerpt());
        }
        if (request.getContentHtml() != null) {
            post.setContentHtml(HTML_POLICY.sanitize(request.getContentHtml()));
        }
        if (request.getCoverImageIds() != null) {
            post.setCoverImageIds(new ArrayList<>(request.getCoverImageIds()));
        }
        if (request.getPublished() != null) {
            post.setPublished(request.getPublished());
        }
    }

    private String ensureUniqueSlug(String base, Long currentId) throws GenericException {
        if (base.isBlank()) {
            base = "post";
        }
        String candidate = base;
        int suffix = 2;
        while (true) {
            var existing = blogPostRepository.findBySlug(candidate);
            if (existing.isEmpty() || existing.get().getId().equals(currentId)) {
                return candidate;
            }
            candidate = base + "-" + suffix++;
        }
    }

    private String slugify(String input) {
        if (input == null) {
            return "";
        }
        String noWhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = MULTI_HYPHEN.matcher(slug).replaceAll("-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
