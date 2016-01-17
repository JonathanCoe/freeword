package io.darknote.data;

public class Note implements Comparable<Note>
{
	private long id;
	private String title;
	private String body;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                '}';
    }

    /**
     * Used to sort Notes by their ID, giving the
     * one with the lowest ID first.
     */
    @Override
    public int compareTo(Note note) {
        return (int) (note.getId() - id);
    }
}