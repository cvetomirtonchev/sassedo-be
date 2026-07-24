package server.sassedo.user.data.projection;

/**
 * Lightweight identity fields shown to admins when reviewing a listing report.
 */
public interface UserReportIdentity {

    Long getId();

    String getName();

    String getEmail();
}
