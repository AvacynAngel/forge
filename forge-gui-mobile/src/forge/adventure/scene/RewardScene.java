package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.character.ShopActor;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.CardUtil;
import forge.adventure.util.Current;
import forge.adventure.util.Reward;
import forge.adventure.util.RewardActor;
import forge.adventure.world.WorldSave;
import forge.assets.ImageCache;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;

/**
 * Displays the rewards of a fight or a treasure
 */
public class RewardScene extends UIScene {
    private TextButton doneButton;
    private Label goldLabel;
    public enum Type {
        Shop,
        Loot
    }

    Type type;
    Array<Actor> generated = new Array<>();
    static public final float CARD_WIDTH =550f ;
    static public final float CARD_HEIGHT = 400f;
    static public final float CARD_WIDTH_TO_HEIGHT = CARD_WIDTH / CARD_HEIGHT;

    public RewardScene() {
        super(Forge.isLandscapeMode() ? "ui/items.json" : "ui/items_portrait.json");
    }

    boolean doneClicked = false;
    float flipCountDown = 1.0f;

    public boolean done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        if (doneClicked)
            return true;

        if (type == Type.Loot) {

            boolean wait = false;
            for (Actor actor : new Array.ArrayIterator<>(generated)) {
                if (!(actor instanceof RewardActor)) {
                    continue;
                }
                RewardActor reward = (RewardActor) actor;
                AdventurePlayer.current().addReward(reward.getReward());
                if (!reward.isFlipped()) {
                    wait = true;
                    reward.flip();
                }
            }
            if (wait) {
                flipCountDown = 1.5f;
                doneClicked = true;
            } else {
                Forge.switchToLast();
            }
        } else {
            Forge.switchToLast();
        }
        return true;
    }

    @Override
    public void act(float delta) {

        stage.act(delta);
        ImageCache.allowSingleLoad();
        if (doneClicked) {
            if (type == Type.Loot)
                flipCountDown -= Gdx.graphics.getDeltaTime();
            if (flipCountDown <= 0) {
                Forge.switchToLast();
            }
        }
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
            goldLabel=ui.findActor("gold");
            ui.onButtonPress("done", new Runnable() {
                @Override
                public void run() {
                    RewardScene.this.done();
                }
            });
            doneButton = ui.findActor("done");
    }

    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            done();
        }
        return true;
    }


    public void loadRewards(Array<Reward> newRewards, Type type, ShopActor shopActor) {
        this.type = type;
        doneClicked = false;


        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            actor.remove();
            if (actor instanceof RewardActor) {
                ((RewardActor) actor).dispose();
            }
        }
        generated.clear();


        Actor card = ui.findActor("cards");
        if(type==Type.Shop)
        {
            goldLabel.setText("Gold:"+Current.player().getGold());
            Actor background = ui.findActor("market_background");
            if(background!=null)
                background.setVisible(true);
        }
        else
        {
            goldLabel.setText("");
            Actor background = ui.findActor("market_background");
            if(background!=null)
                background.setVisible(false);
        }
        // card.setDrawable(new TextureRegionDrawable(new Texture(Res.CurrentRes.GetFile("ui/transition.png"))));

        float targetWidth = card.getWidth();
        float targetHeight = card.getHeight();
        float xOff = card.getX();
        float yOff = card.getY();

        int numberOfRows = 0;
        float cardWidth = 0;
        float cardHeight = 0;
        float bestCardHeight = 0;
        int numberOfColumns = 0;
        float targetArea = targetHeight * targetWidth;
        float oldCardArea = 0;
        float newArea = 0;

        switch (type) {
            case Shop:
                doneButton.setText(Forge.getLocalizer().getMessage("lblLeave"));
                goldLabel.setText("Gold:"+Current.player().getGold());
                break;
            case Loot:
                goldLabel.setText("");
                doneButton.setText(Forge.getLocalizer().getMessage("lblDone"));
                break;
        }
        for (int h = 1; h < targetHeight; h++) {
            cardHeight = h;
            if (type == Type.Shop) {
                cardHeight += doneButton.getHeight();
            }
            //cardHeight=targetHeight/i;
            cardWidth = h / CARD_WIDTH_TO_HEIGHT;
            newArea = newRewards.size * cardWidth * cardHeight;

            int rows = (int) (targetHeight / cardHeight);
            int cols = (int) Math.ceil(newRewards.size / (double) rows);
            if (newArea > oldCardArea && newArea <= targetArea && rows * cardHeight < targetHeight && cols * cardWidth < targetWidth) {
                oldCardArea = newArea;
                numberOfRows = rows;
                numberOfColumns = cols;
                bestCardHeight = h;
            }
        }

        cardHeight = bestCardHeight * 0.90f ;
        cardWidth = bestCardHeight / CARD_WIDTH_TO_HEIGHT;

        yOff += (targetHeight - (cardHeight * numberOfRows)) / 2f;
        xOff += (targetWidth - (cardWidth * numberOfColumns)) / 2f;

        float spacing = 2;
        int i = 0;
        for (Reward reward : new Array.ArrayIterator<>(newRewards)) {
            boolean skipCard = false;
            if (type == Type.Shop) {
                if (shopActor.getMapStage().getChanges().wasCardBought(shopActor.getObjectId(), i)) {
                    skipCard = true;
                }
            }


            int currentRow = (i / numberOfColumns);
            float lastRowXAdjust = 0;
            if (currentRow == numberOfRows - 1) {
                int lastRowCount = newRewards.size % numberOfColumns;
                if (lastRowCount != 0)
                    lastRowXAdjust = ((numberOfColumns * cardWidth) - (lastRowCount * cardWidth)) / 2;
            }
            RewardActor actor = new RewardActor(reward, type == Type.Loot);
            actor.setBounds(lastRowXAdjust + xOff + cardWidth * (i % numberOfColumns) + spacing, yOff + cardHeight * currentRow + spacing, cardWidth - spacing * 2, cardHeight - spacing * 2);

            if (type == Type.Shop) {
                if (currentRow != ((i + 1) / numberOfColumns))
                    yOff += doneButton.getHeight();


                TextButton buyCardButton = new BuyButton(shopActor.getObjectId(), i, shopActor.isUnlimited()?null:shopActor.getMapStage().getChanges(), actor, doneButton);

                generated.add(buyCardButton);
                if (!skipCard) {
                    stage.addActor(buyCardButton);
                }
            }
            generated.add(actor);
            if (!skipCard) {
                stage.addActor(actor);
            }
            i++;
        }
        updateBuyButtons();
    }

    private void updateBuyButtons() {
        for (Actor actor : new Array.ArrayIterator<>(generated)) {
            if (actor instanceof BuyButton) {
                ((BuyButton) actor).update();
            }
        }
    }

    private class BuyButton extends TextButton {
        private final int objectID;
        private final int index;
        private final PointOfInterestChanges changes;
        RewardActor reward;
        int price;

        void update() {
            setDisabled(WorldSave.getCurrentSave().getPlayer().getGold() < price);
        }

        public BuyButton(int id, int i, PointOfInterestChanges ch, RewardActor actor, TextButton style) {
            super("", style.getStyle());
            this.objectID = id;
            this.index = i;
            this.changes = ch;
            reward = actor;
            setHeight(style.getHeight());
            setWidth(actor.getWidth());
            setX(actor.getX());
            setY(actor.getY() - getHeight());
            price = CardUtil.getRewardPrice(actor.getReward());
            setText("$ " + price);
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                if (Current.player().getGold() >= price) {
                    if(changes!=null)
                        changes.buyCard(objectID, index);
                    Current.player().takeGold(price);
                    Current.player().addReward(reward.getReward());

                    Gdx.input.vibrate(5);
                    SoundSystem.instance.play(SoundEffectType.FlipCoin, false);

                    updateBuyButtons();
                    goldLabel.setText("Gold: " + String.valueOf(AdventurePlayer.current().getGold()));
                    if(changes==null)
                        return;
                    setDisabled(true);
                    reward.sold();
                    getColor().a = 0.5f;
                    setText("SOLD");
                    removeListener(this);
                }
                }
            });
        }
    }
}
