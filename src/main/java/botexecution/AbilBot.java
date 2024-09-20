package botexecution;

import common.*;

import org.telegram.telegrambots.abilitybots.api.objects.*;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Dice;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import static java.lang.Integer.parseInt;
import static org.telegram.telegrambots.abilitybots.api.objects.Locality.*;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.*;
import static org.telegram.telegrambots.abilitybots.api.util.AbilityUtils.getChatId;

public class AbilBot extends AbilityBot {
    public String sectionId = "";

    public AbilBot() throws IOException {
        super(new OkHttpTelegramClient(DataReader.readToken()), "Faerie");
        super.onRegister();
    }

    @Override
    public long creatorId() {
        long id;
        try {
            id = DataReader.readCreatorId();
        } catch (IOException e) {
            id = 0;
        }
        return id;
    }

    public void search(MessageContext ctx) {
        SendMessage search = new SendMessage(ctx.chatId().toString(), Constants.SEARCH_MESSAGE);
        search.setReplyMarkup(KeyboardFactory.searchEngine());
        silent.execute(search);
    }

    public void roll(MessageContext ctx) {
        SendMessage roll = new SendMessage(ctx.chatId().toString(), Constants.ROLL_MESSAGE);
        roll.setReplyMarkup(KeyboardFactory.rollVariants());
        silent.execute(roll);
    }

    public void rollAdvantage(Update update) {
        SendMessage rollAdv = new SendMessage(getChatId(update).toString(), Constants.ROLL_MESSAGE_ADVANTAGE);
        rollAdv.setReplyMarkup(KeyboardFactory.rollAdvantage());
        silent.execute(rollAdv);
    }

    public void articleMessaging(ArrayList<String> article, Update update) {
        StringBuilder partOfArticle = new StringBuilder();
        int lengthOfMessage = 0;

        for (String paragraph: article) {
            if (lengthOfMessage + paragraph.length() <= 4095) {
                partOfArticle.append(paragraph);
                lengthOfMessage = lengthOfMessage + paragraph.length();
            }
            else {
                silent.send(partOfArticle.toString(), getChatId(update));
                partOfArticle.setLength(0);
                lengthOfMessage = paragraph.length();
                partOfArticle.append(paragraph);
            }
        }
        silent.send(partOfArticle.toString(), getChatId(update));
    }

    public Ability showHelp() {
        Consumer<MessageContext> helpHand = ctx ->
                silent.send(Constants.HELP_MESSAGE, ctx.chatId());

        return Ability.builder()
                .name("help")
                .info("shows all commands")
                .input(0)
                .locality(USER)
                .privacy(PUBLIC)
                .action(helpHand)
                .build();
    }

    public Ability sayHelloWorld() {
        Consumer<MessageContext> hello = ctx ->
                silent.send("Hello, world!", ctx.chatId());
        Consumer<MessageContext> bye = ctx ->
                silent.send("Bye, world~", ctx.chatId());

        return Ability
                .builder()
                .name("hello")
                .info("says hello world!")
                .input(0)
                .locality(USER)
                .privacy(PUBLIC)
                .action(hello)
                .post(bye)
                .build();
    }

    public Ability requestArticle() {
        //Consumer<MessageContext> search = ctx -> silent.send(SiteParser.SpellsItemsBestiaryGrabber(ctx.firstArg(), ctx.secondArg()), ctx.chatId());
        Consumer<MessageContext> search = this::search;

        return Ability
                .builder()
                .name("search")
                .info("searches article on DnD.su")
                .input(0)
                .locality(USER)
                .privacy(PUBLIC)
                .action(search)
                .build();
    }

    public Ability diceRoll() {
        Consumer<MessageContext> roll = this::roll;

        return Ability
                .builder()
                .name("roll")
                .info("rolls a dice")
                .input(0)
                .locality(USER)
                .privacy(PUBLIC)
                .action(roll)
                .build();
    }

    @Override
    public void consume(Update update) {
        super.consume(update);

        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            String responseQuery = query.getData();

            if (Objects.equals(responseQuery, Constants.SPELLS)) {
                silent.send(Constants.SEARCH_MESSAGE_SPELLS, getChatId(update));
                sectionId = "spells";
            }

            else if (Objects.equals(responseQuery, Constants.ITEMS)) {
                silent.send(Constants.SEARCH_MESSAGE_ITEMS, getChatId(update));
                sectionId = "items";
            }

            else if (Objects.equals(responseQuery, Constants.BESTIARY)) {
                silent.send(Constants.SEARCH_MESSAGE_BESTIARY, getChatId(update));
                sectionId = "bestiary";
            }

            else if (Objects.equals(responseQuery, Constants.ROLL_D20)) {
                silent.send(DiceNew.D20(), getChatId(update));
            }

            else if (Objects.equals(responseQuery, Constants.ROLL_2D20)) {
                rollAdvantage(update);
            }

            else if (Objects.equals(responseQuery, Constants.ROLL_D8)) {
                silent.send(DiceNew.D8(), getChatId(update));
            }

            else if (Objects.equals(responseQuery, Constants.ROLL_D6)) {
                silent.send(DiceNew.D6(), getChatId(update));
            }

            else if (Objects.equals(responseQuery, Constants.ROLL_4D6)) {
                silent.send(DiceNew.D6_four_times(), getChatId(update));
            }

            else if (Objects.equals(responseQuery, Constants.ROLL_D4)) {
                silent.send(DiceNew.D4(), getChatId(update));
            }

            else if (Objects.equals(responseQuery, Constants.ADVANTAGE)) {
                silent.send(DiceNew.D20_two_times(true), getChatId(update));
            }

            else if (Objects.equals(responseQuery, Constants.DISADVANTAGE)) {
                silent.send(DiceNew.D20_two_times(false), getChatId(update));
            }
        }

        if (update.hasMessage() && update.getMessage().hasText() && !update.getMessage().isCommand()) {
            if (!sectionId.isEmpty()) {
                articleMessaging(SiteParser.SpellsItemsBestiaryGrabber(sectionId, update.getMessage().getText()), update);
                sectionId = "";
            }
        }
    }
}