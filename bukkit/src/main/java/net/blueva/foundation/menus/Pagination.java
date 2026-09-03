package net.blueva.foundation.menus;

/**
 * Page arithmetic for a paginated menu, and the placeholders that go with it.
 *
 * <p>Pages are zero-based in code and one-based in text, which is the only
 * sane split: {@code entries.subList(start, end)} wants the former and a
 * player reading "Page 0 of 3" wants the latter.</p>
 */
public final class Pagination {

    private final int page;
    private final int pageSize;
    private final int total;

    /**
     * @param page     the current page, zero-based; clamped into range
     * @param pageSize how many entries fit on a page
     * @param total    how many entries there are
     */
    public Pagination(int page, int pageSize, int total) {
        this.pageSize = Math.max(1, pageSize);
        this.total = Math.max(0, total);
        this.page = Math.max(0, Math.min(page, totalPages(this.total, this.pageSize) - 1));
    }

    private static int totalPages(int total, int pageSize) {
        if (total <= 0) {
            return 1;
        }
        return (total + pageSize - 1) / pageSize;
    }

    /**
     * @return the current page, zero-based
     */
    public int page() {
        return page;
    }

    /**
     * @return how many entries fit on a page
     */
    public int pageSize() {
        return pageSize;
    }

    /**
     * @return how many entries there are in total
     */
    public int total() {
        return total;
    }

    /**
     * @return how many pages there are, at least one
     */
    public int totalPages() {
        return totalPages(total, pageSize);
    }

    /**
     * @return the index of this page's first entry
     */
    public int start() {
        return page * pageSize;
    }

    /**
     * @return the index just past this page's last entry
     */
    public int end() {
        return Math.min(start() + pageSize, total);
    }

    /**
     * @return whether there is a page after this one
     */
    public boolean hasNext() {
        return page < totalPages() - 1;
    }

    /**
     * @return whether there is a page before this one
     */
    public boolean hasPrevious() {
        return page > 0;
    }

    /**
     * @return the next page, or this one if there is none
     */
    public int next() {
        return hasNext() ? page + 1 : page;
    }

    /**
     * @return the previous page, or this one if there is none
     */
    public int previous() {
        return hasPrevious() ? page - 1 : page;
    }

    /**
     * Replace the page placeholders in a line of text.
     *
     * <p>Recognised: {@code {page}}, {@code {page_total}}, {@code {page_next}},
     * {@code {page_previous}}, {@code {page_has_next}},
     * {@code {page_has_previous}}, {@code {entries_total}}.</p>
     *
     * @param text the text, may be {@code null}
     * @return the text with placeholders filled in
     */
    public String apply(String text) {
        if (text == null || text.indexOf('{') < 0) {
            return text;
        }
        return text
                .replace("{page}", Integer.toString(page + 1))
                .replace("{page_total}", Integer.toString(totalPages()))
                .replace("{page_next}", Integer.toString(next() + 1))
                .replace("{page_previous}", Integer.toString(previous() + 1))
                .replace("{page_has_next}", Boolean.toString(hasNext()))
                .replace("{page_has_previous}", Boolean.toString(hasPrevious()))
                .replace("{entries_total}", Integer.toString(total));
    }
}
