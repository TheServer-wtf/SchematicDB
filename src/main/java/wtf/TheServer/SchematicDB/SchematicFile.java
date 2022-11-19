package wtf.TheServer.SchematicDB;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;

public class SchematicFile {
    private final String name;
    private final UUID user;
    private final long created;
    private final long edited;
    private final ArrayList<Byte> file = new ArrayList<>();

    public SchematicFile(@NotNull String name, @NotNull UUID user, long created, long edited, @NotNull ArrayList<Byte> bytes) {
        this.name = name;
        this.user = user;
        this.created = created;
        this.edited = edited;
        file.addAll(bytes);
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public UUID getUser() {
        return user;
    }

    public long createTime() {
        return created;
    }

    public long lastEdited() {
        return edited;
    }

    @NotNull
    public ArrayList<Byte> getFile() {
        return file;
    }
}
