package za.co.entelect.challenge.command;

public class SelectCommand implements Command {
    private final int target;
    private int x;
    private int y;
    private int followUpCommand;

    public SelectCommand(int target, int followUpCommand,int x, int y) {
        this.target = target;
        this.x = x;
        this.y = y;
        this.followUpCommand = followUpCommand;
    }

    @Override
    public String render()
    {
        switch(followUpCommand)
        {
            case 5:
                return String.format("select %d;banana %d %d", target, x, y);
            case 6:
                return String.format("select %d;snowball %d %d", target, x, y);
            default:
                return String.format("select %d;", target);
        }
    }
}
