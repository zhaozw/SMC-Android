package rs.pedjaapps.smc.object.enemy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import rs.pedjaapps.smc.Assets;
import rs.pedjaapps.smc.object.GameObject;
import rs.pedjaapps.smc.object.Maryo;
import rs.pedjaapps.smc.object.Sprite;
import rs.pedjaapps.smc.object.World;
import rs.pedjaapps.smc.screen.GameScreen;
import rs.pedjaapps.smc.utility.Constants;
import rs.pedjaapps.smc.utility.Utility;

/**
 * Created by pedja on 18.5.14..
 */
public class Krush extends Enemy
{
    public static final float VELOCITY_SMALL = 2.75f;
    public static final float VELOCITY_BIG = 1.5f;
    public static final int KP_SMALL = 20;
    public static final int KP_BIG = 40;
    public static final float POS_Z = 0.09f;

    boolean dying = false;

    public boolean isSmall;

    private String keyDead, keySmall, keyBig;

    public Krush(World world, Vector2 size, Vector3 position)
    {
        super(world, size, position);
        setupBoundingBox();
        position.z = POS_Z;
    }

    @Override
    public void initAssets()
    {
        keyDead = textureAtlas + ":dead";
        keySmall = textureAtlas + ":small";
        keyBig = textureAtlas + ":big";
        TextureAtlas atlas = Assets.manager.get(textureAtlas);

        Array<TextureRegion> smallFrames = new Array<>();
        Array<TextureRegion> bigFrames = new Array<>();

        bigFrames.add(atlas.findRegion("big-1"));
        bigFrames.add(atlas.findRegion("big-2"));
        bigFrames.add(atlas.findRegion("big-3"));
        bigFrames.add(atlas.findRegion("big-4"));

        smallFrames.add(atlas.findRegion("small-1"));
        smallFrames.add(atlas.findRegion("small-2"));
        smallFrames.add(atlas.findRegion("small-3"));
        smallFrames.add(atlas.findRegion("small-4"));

        Assets.animations.put(keySmall, new Animation(0.07f, smallFrames));
        Assets.animations.put(keyBig, new Animation(0.12f, bigFrames));

        TextureRegion tmp = atlas.findRegion("small-1");
        Assets.loadedRegions.put(keyDead, tmp);
    }

    @Override
    public void draw(SpriteBatch spriteBatch)
    {
        TextureRegion frame;
        if (!dying)
        {
            frame = Assets.animations.get(isSmall ? keySmall : keyBig).getKeyFrame(stateTime, true);
            frame.flip(direction == Direction.left, false);
            Utility.draw(spriteBatch, frame, mDrawRect.x, mDrawRect.y, mDrawRect.height);
            frame.flip(direction == Direction.left, false);
        }
        else
        {
            frame = Assets.animations.get(keySmall).getKeyFrames()[0];
            frame.flip(direction == Direction.left, false);
            spriteBatch.draw(frame, mDrawRect.x , mDrawRect.y , mDrawRect.width, mDrawRect.height);
            frame.flip(direction == Direction.left, false);
        }
    }

    @Override
    public void update(float deltaTime)
    {
        stateTime += deltaTime;
        if(dying)
        {
            //resize it by state time
            mDrawRect.height -= Gdx.graphics.getFramesPerSecond() * 0.00035;
            mDrawRect.width -= Gdx.graphics.getFramesPerSecond() * 0.000175;
            if(mDrawRect.height < 0)world.trashObjects.add(this);
            return;
        }

        // Setting initial vertical acceleration
        acceleration.y = Constants.GRAVITY;

        // Convert acceleration to frame time
        acceleration.scl(deltaTime);

        // apply acceleration to change velocity
        velocity.add(acceleration);

        checkCollisionWithBlocks(deltaTime, !deadByBullet, !deadByBullet);

        if (!deadByBullet)
        {
            switch(direction)
            {
                case right:
                    velocity.set(velocity.x = -(isSmall ? VELOCITY_SMALL : VELOCITY_BIG), velocity.y, velocity.z);
                    break;
                case left:
                    velocity.set(velocity.x = (isSmall ? VELOCITY_SMALL : VELOCITY_BIG), velocity.y, velocity.z);
                    break;
            }
        }
    }

    @Override
    protected void handleCollision(GameObject object, boolean vertical)
    {
        super.handleCollision(object, vertical);
        if(!vertical)
        {
            if(((object instanceof Sprite && ((Sprite)object).type == Sprite.Type.massive
                    && object.mColRect.y + object.mColRect.height > mColRect.y + 0.1f)
                    || object instanceof EnemyStopper
                    || (object instanceof Enemy && this != object)))
            {
                //CollisionManager.resolve_objects(this, object, true);
                handleCollision(Enemy.ContactType.stopper);
            }
        }
    }

    @Override
    public void handleCollision(Enemy.ContactType contactType)
    {
        switch(contactType)
        {
            case stopper:
                direction = direction == Direction.right ? Direction.left : Direction.right;
                velocity.x = velocity.x > 0 ? -velocity.x : Math.abs(velocity.x);
                break;
        }
    }

    private void setupBoundingBox()
    {
        mColRect.height = mColRect.height - 0.2f;
    }

    @Override
    public void updateBounds()
    {
        mDrawRect.height = mColRect.height + 0.2f;
        super.updateBounds();
    }

    @Override
    public int hitByPlayer(Maryo maryo, boolean vertical)
    {
        if (maryo.velocity.y < 0 && vertical && maryo.mColRect.y > mColRect.y)//enemy death from above
        {
            if(isSmall)
            {
                ((GameScreen) world.screen).killPointsTextHandler.add(isSmall ? KP_SMALL : KP_BIG, position.x, position.y + mDrawRect.height);
                stateTime = 0;
                handleCollision = false;
                dying = true;
                Sound sound = Assets.manager.get("data/sounds/enemy/furball/die.ogg");
                if (sound != null && Assets.playSounds)
                {
                    sound.play();
                }
            }
            else
            {
                isSmall = true;
            }
            return HIT_RESOLUTION_ENEMY_DIED;
        }
        else
        {
            return HIT_RESOLUTION_PLAYER_DIED;
        }
    }

    @Override
    protected TextureRegion getDeadTextureRegion()
    {
        return Assets.loadedRegions.get(keyDead);
    }

    @Override
    public void downgradeOrDie(GameObject killedBy)
    {
        super.downgradeOrDie(killedBy);
        ((GameScreen)world.screen).killPointsTextHandler.add(isSmall ? KP_SMALL : KP_BIG, position.x, position.y + mDrawRect.height);
    }
}