package com.collective404.budget.service;

import com.collective404.budget.dto.BudgetReportResponse;

/**
 * Outcome of a budget report request: either the report, or the HTTP status
 * and reason explaining why the caller can't have it.
 *
 * @param status the HTTP status code for the response
 * @param reason why the request failed, or null on success
 * @param report the report on success, or null on failure
 */
public record BudgetReportResult(
        int status,
        String reason,
        BudgetReportResponse report
) {

    /** Status code for a successful report. */
    public static final int OK = 200;

    /** Status code when the API key is missing or unknown. */
    public static final int UNAUTHORIZED = 401;

    /** Status code when the budget belongs to another client. */
    public static final int FORBIDDEN = 403;

    /** Status code when no budget has the requested ID. */
    public static final int NOT_FOUND = 404;

    /**
     * Builds a successful result.
     *
     * @param report the report
     * @return the result
     */
    public static BudgetReportResult ok(final BudgetReportResponse report) {
        return new BudgetReportResult(OK, null, report);
    }

    /**
     * Builds a failed result.
     *
     * @param status the HTTP status code
     * @param reason why the request failed
     * @return the result
     */
    public static BudgetReportResult error(
            final int status, final String reason) {
        return new BudgetReportResult(status, reason, null);
    }

    /**
     * Whether the caller gets the report.
     *
     * @return true if the request succeeded
     */
    public boolean isOk() {
        return status == OK;
    }
}
