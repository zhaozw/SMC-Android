package rs.pedjaapps.smc.object.maryo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.util.Collections;

import rs.pedjaapps.smc.Assets;
import rs.pedjaapps.smc.object.Box;
import rs.pedjaapps.smc.object.DynamicObject;
import rs.pedjaapps.smc.object.GameObject;
import rs.pedjaapps.smc.object.LevelEntry;
import rs.pedjaapps.smc.object.LevelExit;
import rs.pedjaapps.smc.object.Sprite;
import rs.pedjaapps.smc.object.World;
import rs.pedjaapps.smc.object.enemy.Eato;
import rs.pedjaapps.smc.object.enemy.Enemy;
import rs.pedjaapps.smc.object.enemy.Flyon;
import rs.pedjaapps.smc.object.enemy.Spika;
import rs.pedjaapps.smc.object.enemy.Thromp;
import rs.pedjaapps.smc.object.items.Item;
import rs.pedjaapps.smc.screen.AbstractScreen;
import rs.pedjaapps.smc.screen.GameScreen;
import rs.pedjaapps.smc.screen.LoadingScreen;
import rs.pedjaapps.smc.utility.GameSaveUtility;
import rs.pedjaapps.smc.utility.LevelLoader;

public class Maryo extends DynamicObject
{
    public enum MaryoState
    {
        small, big, fire, ice, ghost, flying
    }

    //this could be all done dynamically, but this way we minimize allocation in game loop
    //omg, this is a lot of constants :D
    private static final int A_KEY_WALKING_SMALL = 0;
    private static final int A_KEY_CLIMB_SMALL = 1;
    private static final int A_KEY_WALKING_BIG = 2;
    private static final int A_KEY_CLIMB_BIG = 3;
    private static final int A_KEY_WALKING_FIRE = 4;
    private static final int A_KEY_THROW_FIRE = 5;
    private static final int A_KEY_CLIMB_FIRE = 6;
    private static final int A_KEY_WALKING_FLYING = 7;
    private static final int A_KEY_CLIMB_FLYING = 8;
    private static final int A_KEY_WALKING_GHOST = 9;
    private static final int A_KEY_CLIMB_GHOST = 10;
    private static final int A_KEY_WALKING_ICE = 11;
    private static final int A_KEY_THROW_ICE = 12;
    private static final int A_KEY_CLIMB_ICE = 13;

    private static final int T_KEY_DUCK_RIGHT_SMALL = 0;
    private static final int T_KEY_JUMP_RIGHT_SMALL = 1;
    private static final int T_KEY_FALL_RIGHT_SMALL = 2;
    private static final int T_KEY_DEAD_RIGHT_SMALL = 3;
    private static final int T_KEY_STAND_RIGHT_SMALL = 4;

    private static final int T_KEY_DUCK_RIGHT_BIG = 5;
    private static final int T_KEY_JUMP_RIGHT_BIG = 6;
    private static final int T_KEY_FALL_RIGHT_BIG = 7;
    private static final int T_KEY_DEAD_RIGHT_BIG = 8;
    private static final int T_KEY_STAND_RIGHT_BIG = 9;

    private static final int T_KEY_DUCK_RIGHT_FIRE = 10;
    private static final int T_KEY_JUMP_RIGHT_FIRE = 11;
    private static final int T_KEY_FALL_RIGHT_FIRE = 12;
    private static final int T_KEY_DEAD_RIGHT_FIRE = 13;
    private static final int T_KEY_STAND_RIGHT_FIRE = 14;

    private static final int T_KEY_DUCK_RIGHT_FLYING = 15;
    private static final int T_KEY_JUMP_RIGHT_FLYING = 16;
    private static final int T_KEY_FALL_RIGHT_FLYING = 17;
    private static final int T_KEY_DEAD_RIGHT_FLYING = 18;
    private static final int T_KEY_STAND_RIGHT_FLYING = 19;

    private static final int T_KEY_DUCK_RIGHT_GHOST = 20;
    private static final int T_KEY_JUMP_RIGHT_GHOST = 21;
    private static final int T_KEY_FALL_RIGHT_GHOST = 22;
    private static final int T_KEY_DEAD_RIGHT_GHOST = 23;
    private static final int T_KEY_STAND_RIGHT_GHOST = 24;

    private static final int T_KEY_DUCK_RIGHT_ICE = 25;
    private static final int T_KEY_JUMP_RIGHT_ICE = 26;
    private static final int T_KEY_FALL_RIGHT_ICE = 27;
    private static final int T_KEY_DEAD_RIGHT_ICE = 28;
    private static final int T_KEY_STAND_RIGHT_ICE = 29;

    public static final float POSITION_Z = 0.0999f;

    private static final float RUNNING_FRAME_DURATION = 0.08f;
    private static final float CLIMB_FRAME_DURATION = 0.25f;
    private static final float THROW_FRAME_DURATION = 0.1f;
    private static final float RESIZE_ANIMATION_DURATION = 0.977f;
    private static final float RESIZE_ANIMATION_FRAME_DURATION = RESIZE_ANIMATION_DURATION / 8f;

    protected static final float MAX_VEL = 4f;
    private static final float GOD_MOD_TIMEOUT = 3000;//3 sec

    private static final float BULLET_COOLDOWN = 1f;//1 sec

    WorldState worldState = WorldState.JUMPING;
    private MaryoState maryoState = GameSaveUtility.getInstance().save.playerState;
    public boolean facingLeft = false;
    public boolean longJump = false;

    public float groundY = 0;

    private boolean handleCollision = true;
    DyingAnimation dyingAnim = new DyingAnimation();

    public Sound jumpSound = null;

    public Rectangle debugRayRect = new Rectangle();

    /**
     * Makes player invincible and transparent for all enemies
     * Used (for limited time) when player is downgraded (or if you hack the game :D
     */
    boolean godMode = false;
    long godModeActivatedTime;

    Animation resizingAnimation;
    float resizeAnimStartTime;
    private MaryoState newState;//used with resize animation
    private MaryoState oldState;//used with resize animation

    //exit, enter
    public float enterStartTime;
    public boolean exiting, entering;
    private LevelExit exit;
    private LevelEntry entry;
    private Vector3 exitEnterStartPosition = new Vector3();
    private static final float exitEnterVelocity = 1.3f;
    private int rotation = 0;
    public ParticleEffect powerJumpEffect;
    public boolean powerJump;
    private float bulletShotTime = BULLET_COOLDOWN;
    private boolean fire;
    private float fireAnimationStateTime;

    //textures
    private TextureRegion[] tMap = new TextureRegion[30];
    private Animation[] aMap = new Animation[14];

    public Maryo(World world, Vector3 position, Vector2 size)
    {
        super(world, size, position);
        setupBoundingBox();

        position.y = mColRect.y = mDrawRect.y += 0.5f;
        Assets.manager.load("data/animation/particles/maryo_power_jump_emitter.p", ParticleEffect.class, Assets.particleEffectParameter);
    }

    private void setupBoundingBox()
    {
        float centerX = position.x + mColRect.width / 2;
        switch (maryoState)
        {
            case small:
                mDrawRect.width = 0.9f;
                mDrawRect.height = 0.9f;
                break;
            case big:
            case fire:
            case ghost:
            case ice:
                mDrawRect.height = 1.09f;
                mDrawRect.width = 1.09f;
                break;
            case flying:
                break;
        }
        mColRect.x = mDrawRect.x + mDrawRect.width / 4;
        mColRect.width = mDrawRect.width / 2;
        position.x = mColRect.x;

        if (worldState == WorldState.DUCKING)
        {
            mColRect.height = mDrawRect.height / 2;
        }
        else
        {
            mColRect.height = mDrawRect.height * 0.9f;
        }

        position.x = mColRect.x = centerX - mColRect.width / 2;
    }

    @Override
    public void updateBounds()
    {
        mDrawRect.x = (world.screen.getTimeStep() == AbstractScreen.FIXED_TIMESTEP ? interpPosition.x : mColRect.x) - mDrawRect.width / 4;
        mDrawRect.y = world.screen.getTimeStep() == AbstractScreen.FIXED_TIMESTEP ? interpPosition.y : mColRect.y;
    }

    public void initAssets()
    {
        MaryoState[] states = new MaryoState[]{MaryoState.small, MaryoState.big, MaryoState.fire, MaryoState.ghost, MaryoState.ice};
        for (MaryoState ms : states)
        {
            loadTextures(ms);
        }
        setJumpSound();
        powerJumpEffect = new ParticleEffect(Assets.manager.get("data/animation/particles/maryo_power_jump_emitter.p", ParticleEffect.class));

    }

    private void loadTextures(MaryoState state)
    {
        TextureAtlas atlas = Assets.manager.get("data/maryo/" + state + ".pack");

        TextureRegion tmpStandRight;
        tMap[tIndex(state, TKey.stand_right)] = tmpStandRight = atlas.findRegion(TKey.stand_right.toString());

        TextureRegion[] walkFrames = new TextureRegion[4];
        walkFrames[0] = tmpStandRight;
        walkFrames[1] = atlas.findRegion(TKey.walk_right_1 + "");
        walkFrames[2] = atlas.findRegion(TKey.walk_right_2 + "");
        walkFrames[3] = walkFrames[1];
        aMap[aIndex(state, AKey.walk)] = new Animation(RUNNING_FRAME_DURATION, walkFrames);

        TextureRegion[] climbFrames = new TextureRegion[2];
        climbFrames[0] = atlas.findRegion(TKey.climb_left + "");
        climbFrames[1] = atlas.findRegion(TKey.climb_right + "");
        aMap[aIndex(state, AKey.climb)] = new Animation(CLIMB_FRAME_DURATION, climbFrames);

        if (state == MaryoState.ice || state == MaryoState.fire)
        {
            TextureRegion[] throwFrames = new TextureRegion[2];
            throwFrames[0] = atlas.findRegion(TKey.throw_right_1.toString());
            throwFrames[1] = atlas.findRegion(TKey.throw_right_2.toString());
            aMap[aIndex(state, AKey._throw)] = new Animation(THROW_FRAME_DURATION, throwFrames);
        }

        tMap[tIndex(state, TKey.jump_right)] = atlas.findRegion(TKey.jump_right.toString());
        tMap[tIndex(state, TKey.fall_right)] = atlas.findRegion(TKey.fall_right.toString());

        if (MaryoState.small == state)
        {
            tMap[tIndex(state, TKey.dead_right)] = atlas.findRegion(TKey.dead_right.toString());
        }

        tMap[tIndex(state, TKey.duck_right)] = atlas.findRegion(TKey.duck_right.toString());
    }

    @Override
    public void _render(SpriteBatch spriteBatch)
    {
        TextureRegion marioFrame;
        if (exiting)
        {
            marioFrame = tMap[tIndex(maryoState, TKey.stand_right)];

            float originX = mDrawRect.width * 0.5f;
            float originY = mDrawRect.height * 0.5f;
            spriteBatch.draw(marioFrame, mDrawRect.x, mDrawRect.y, originX, originY, mDrawRect.width, mDrawRect.height, 1, 1, rotation);

            return;
        }
        if (entering)
        {
            marioFrame = tMap[tIndex(maryoState, TKey.stand_right)];

            float originX = mDrawRect.width * 0.5f;
            float originY = mDrawRect.height * 0.5f;
            spriteBatch.draw(marioFrame, mDrawRect.x, mDrawRect.y, originX, originY, mDrawRect.width, mDrawRect.height, 1, 1, rotation);

            return;
        }
        if (resizingAnimation != null && stateTime > resizeAnimStartTime + RESIZE_ANIMATION_DURATION)
        {
            resizeAnimStartTime = 0;
            resizingAnimation = null;
            ((GameScreen) world.screen).setGameState(GameScreen.GAME_STATE.GAME_RUNNING);
            godMode = false;
            maryoState = newState;
            newState = null;
            oldState = null;
            setupBoundingBox();
            GameSaveUtility.getInstance().save.playerState = maryoState;
        }
        if (resizingAnimation != null)
        {
            int index = resizingAnimation.getKeyFrameIndex(stateTime);
            marioFrame = resizingAnimation.getKeyFrames()[index];
            if (index == 0)
            {
                maryoState = oldState;
                setupBoundingBox();
            }
            else
            {
                maryoState = newState;
                setupBoundingBox();
            }
        }
        else if (fire)
        {
            Animation animation = aMap[aIndex(maryoState, AKey._throw)];
            marioFrame = animation.getKeyFrame(fireAnimationStateTime, false);
            if (animation.isAnimationFinished(fireAnimationStateTime))
            {
                fire = false;
                fireAnimationStateTime = 0;
                //doFire();
            }
        }
        else if (worldState.equals(WorldState.WALKING))
        {
            marioFrame = aMap[aIndex(maryoState, AKey.walk)].getKeyFrame(stateTime, true);
        }
        else if (worldState == WorldState.DUCKING)
        {
            marioFrame = tMap[tIndex(maryoState, TKey.duck_right)];
        }
        else if (getWorldState().equals(WorldState.JUMPING))
        {
            if (velocity.y > 0)
            {
                marioFrame = tMap[tIndex(maryoState, TKey.jump_right)];
            }
            else
            {
                marioFrame = tMap[tIndex(maryoState, TKey.fall_right)];
            }
        }
        else if (worldState == WorldState.DYING)
        {
            marioFrame = tMap[tIndex(maryoState, TKey.dead_right)];
        }
        else if (worldState == WorldState.CLIMBING)
        {
            TextureRegion[] frames = aMap[aIndex(maryoState, AKey.climb)].getKeyFrames();
            float distance = position.y - exitEnterStartPosition.y;
            marioFrame = frames[Math.floor(distance / 0.3f) % 2 == 0 ? 0 : 1];
        }
        else
        {
            marioFrame = tMap[tIndex(maryoState, TKey.stand_right)];
        }

        marioFrame.flip(facingLeft, false);
        //if god mode, make player half-transparent
        if (godMode)
        {
            Color color = spriteBatch.getColor();
            float oldA = color.a;

            color.a = 0.5f;
            spriteBatch.setColor(color);

            spriteBatch.draw(marioFrame, mDrawRect.x, mDrawRect.y, mDrawRect.width, mDrawRect.height);

            color.a = oldA;
            spriteBatch.setColor(color);
        }
        else
        {
            spriteBatch.draw(marioFrame, mDrawRect.x, mDrawRect.y, mDrawRect.width, mDrawRect.height);
        }
        marioFrame.flip(facingLeft, false);
        if (worldState == WorldState.DUCKING && powerJump)
        {
            powerJumpEffect.setPosition(position.x, position.y + 0.05f);
            powerJumpEffect.draw(spriteBatch);
        }
    }

    private int tIndex(MaryoState state, TKey tkey)
    {
        switch (tkey)
        {
            case stand_right:
                switch (state)
                {
                    case small:
                        return T_KEY_STAND_RIGHT_SMALL;
                    case big:
                        return T_KEY_STAND_RIGHT_BIG;
                    case fire:
                        return T_KEY_STAND_RIGHT_FIRE;
                    case ice:
                        return T_KEY_STAND_RIGHT_ICE;
                    case ghost:
                        return T_KEY_STAND_RIGHT_GHOST;
                    case flying:
                        return T_KEY_STAND_RIGHT_FLYING;
                }
                break;
            case jump_right:
                switch (state)
                {
                    case small:
                        return T_KEY_JUMP_RIGHT_SMALL;
                    case big:
                        return T_KEY_JUMP_RIGHT_BIG;
                    case fire:
                        return T_KEY_JUMP_RIGHT_FIRE;
                    case ice:
                        return T_KEY_JUMP_RIGHT_ICE;
                    case ghost:
                        return T_KEY_JUMP_RIGHT_GHOST;
                    case flying:
                        return T_KEY_JUMP_RIGHT_FLYING;
                }
                break;
            case fall_right:
                switch (state)
                {
                    case small:
                        return T_KEY_FALL_RIGHT_SMALL;
                    case big:
                        return T_KEY_FALL_RIGHT_BIG;
                    case fire:
                        return T_KEY_FALL_RIGHT_FIRE;
                    case ice:
                        return T_KEY_FALL_RIGHT_ICE;
                    case ghost:
                        return T_KEY_FALL_RIGHT_GHOST;
                    case flying:
                        return T_KEY_FALL_RIGHT_FLYING;
                }
                break;
            case dead_right:
                switch (state)
                {
                    case small:
                        return T_KEY_DEAD_RIGHT_SMALL;
                    case big:
                        return T_KEY_DEAD_RIGHT_BIG;
                    case fire:
                        return T_KEY_DEAD_RIGHT_FIRE;
                    case ice:
                        return T_KEY_DEAD_RIGHT_ICE;
                    case ghost:
                        return T_KEY_DEAD_RIGHT_GHOST;
                    case flying:
                        return T_KEY_DEAD_RIGHT_FLYING;
                }
                break;
            case duck_right:
                switch (state)
                {
                    case small:
                        return T_KEY_DUCK_RIGHT_SMALL;
                    case big:
                        return T_KEY_DUCK_RIGHT_BIG;
                    case fire:
                        return T_KEY_DUCK_RIGHT_FIRE;
                    case ice:
                        return T_KEY_DUCK_RIGHT_ICE;
                    case ghost:
                        return T_KEY_DUCK_RIGHT_GHOST;
                    case flying:
                        return T_KEY_DUCK_RIGHT_FLYING;
                }
                break;
        }
        throw new IllegalArgumentException("Unknown texture key '" + tkey + "' or maryoState '" + maryoState + "'");
    }

    private int aIndex(MaryoState state, AKey akey)
    {
        switch (state)
        {
            case small:
                switch (akey)
                {
                    case walk:
                        return A_KEY_WALKING_SMALL;
                    case climb:
                        return A_KEY_CLIMB_SMALL;
                }
                break;
            case big:
                switch (akey)
                {
                    case walk:
                        return A_KEY_WALKING_BIG;
                    case climb:
                        return A_KEY_CLIMB_BIG;
                }
                break;
            case fire:
                switch (akey)
                {
                    case walk:
                        return A_KEY_WALKING_FIRE;
                    case climb:
                        return A_KEY_CLIMB_FIRE;
                    case _throw:
                        return A_KEY_THROW_FIRE;
                }
                break;
            case ice:
                switch (akey)
                {
                    case walk:
                        return A_KEY_WALKING_ICE;
                    case climb:
                        return A_KEY_CLIMB_ICE;
                    case _throw:
                        return A_KEY_THROW_ICE;
                }
                break;
            case ghost:
                switch (akey)
                {
                    case walk:
                        return A_KEY_WALKING_GHOST;
                    case climb:
                        return A_KEY_CLIMB_GHOST;
                }
                break;
            case flying:
                switch (akey)
                {
                    case walk:
                        return A_KEY_WALKING_FLYING;
                    case climb:
                        return A_KEY_CLIMB_FLYING;
                }
                break;
        }
        throw new IllegalArgumentException("Unknown animation key '" + akey + "' or maryoState '" + maryoState + "'");
    }

    @Override
    public void _update(float delta)
    {
        if (exiting)
        {
            boolean isDone = false;
            float velDelta = exitEnterVelocity * delta;
            if ("up".equals(exit.direction))
            {
                if (position.y >= exitEnterStartPosition.y + mDrawRect.height)
                {
                    isDone = true;
                }
                else
                {
                    mColRect.y = position.y += mDrawRect.height * velDelta;
                }
            }
            else if ("down".equals(exit.direction))
            {
                if (position.y <= exitEnterStartPosition.y - mDrawRect.height)
                {
                    isDone = true;
                }
                else
                {
                    mColRect.y = position.y -= mDrawRect.height * velDelta;
                }
            }
            else if ("right".equals(exit.direction))
            {
                if (position.x >= exitEnterStartPosition.x + mDrawRect.width)
                {
                    isDone = true;
                }
                else
                {
                    rotation = -90;
                    mColRect.x = position.x += mDrawRect.width * velDelta;
                }
            }
            else if ("left".equals(exit.direction))
            {
                if (exitEnterStartPosition.x - position.x >= mDrawRect.width)
                {
                    isDone = true;
                }
                else
                {
                    rotation = 90;
                    mColRect.x = position.x -= mDrawRect.width * velDelta;
                }
            }
            if (isDone)
            {
                exiting = false;
                //((GameScreen)world.screen).setGameState(GameScreen.GAME_STATE.GAME_RUNNING);

                String nextLevelName;
                if (exit.levelName == null)
                {
                    String currentLevel = ((GameScreen) world.screen).parent == null ? ((GameScreen) world.screen).levelName : ((GameScreen) world.screen).parent.levelName;
                    nextLevelName = GameSaveUtility.getInstance().getNextLevel(currentLevel);
                }
                else
                {
                    nextLevelName = exit.levelName;
                }
                GameScreen parent;
                GameScreen newScreen;
                boolean resume = false;
                if (nextLevelName.contains("sub"))
                {
                    parent = (GameScreen) world.screen;
                    newScreen = new GameScreen(world.screen.game, false, nextLevelName, parent);
                }
                else if (((GameScreen) world.screen).parent != null && nextLevelName.equals(((GameScreen) world.screen).parent.levelName))
                {
                    newScreen = ((GameScreen) world.screen).parent;
                    newScreen.forceCheckEnter = true;
                    resume = true;
                }
                else
                {
                    if (((GameScreen) world.screen).parent != null)
                    {
                        ((GameScreen) world.screen).parent.dispose();
                        ((GameScreen) world.screen).parent = null;
                    }
                    newScreen = new GameScreen(world.screen.game, false, nextLevelName, null);
                }
                world.screen.game.setScreen(new LoadingScreen(newScreen, resume));
            }
            else
            {
                updateBounds();
            }
            return;
        }
        if (entering)
        {
            enterStartTime += delta;
            if (enterStartTime < 1)
            {
                return;
            }
            boolean isDone = false;
            float velDelta = exitEnterVelocity * delta;
            if ("up".equals(entry.direction))
            {
                if (position.y > entry.mColRect.y + entry.mColRect.height)
                {
                    isDone = true;
                }
                else
                {
                    mColRect.y = position.y += mDrawRect.height * velDelta;
                }
            }
            else if ("down".equals(entry.direction))
            {
                if (position.y + mDrawRect.height < entry.mColRect.y)
                {
                    isDone = true;
                }
                else
                {
                    mColRect.y = position.y -= mDrawRect.height * velDelta;
                }
            }
            else if ("right".equals(entry.direction))
            {
                if (position.x > entry.mColRect.x + entry.mColRect.width)
                {
                    isDone = true;
                }
                else
                {
                    rotation = -90;
                    mColRect.x = position.x += mDrawRect.width * velDelta;
                }
            }
            else if ("left".equals(entry.direction))
            {
                if (position.x + mDrawRect.width < entry.mColRect.x)
                {
                    isDone = true;
                }
                else
                {
                    rotation = 90;
                    mColRect.x = position.x -= mDrawRect.width * velDelta;
                }
            }
            if (isDone)
            {
                position.z = POSITION_Z;
                Collections.sort(world.level.gameObjects, new LevelLoader.ZSpriteComparator());
                entering = false;
                ((GameScreen) world.screen).setGameState(GameScreen.GAME_STATE.GAME_RUNNING);
            }
            else
            {
                updateBounds();
            }
            return;
        }
        if (fire)
        {
            fireAnimationStateTime += delta;
        }
        //disable godmod after timeot
        if (godMode && System.currentTimeMillis() - godModeActivatedTime > GOD_MOD_TIMEOUT)
        {
            godMode = false;
        }
        if (worldState == WorldState.DYING)
        {
            stateTime += delta;
            if (dyingAnim.update(delta)) super._update(delta);
        }
        else if (resizingAnimation != null)
        {
            stateTime += delta;
        }
        else
        {
            if (worldState == WorldState.CLIMBING)
            {
                checkCollisionWithBlocks(delta);
                boolean climbing = false;
                Array<GameObject> vo = world.getVisibleObjects();
                for (int i = 0; i < vo.size; i++)
                {
                    GameObject go = vo.get(i);
                    if (go instanceof Sprite && ((Sprite) go).type == Sprite.Type.climbable && go.mColRect.overlaps(mColRect))
                    {
                        climbing = true;
                        break;
                    }
                }
                if (!climbing) setWorldState(WorldState.JUMPING);
                stateTime += delta;
            }
            else
            {
                super._update(delta);
                //TODO ovo se vec proverava u checkY
                //check where ground is
                Array<GameObject> objects = world.getVisibleObjects();
                Rectangle rect = World.RECT_POOL.obtain();
                debugRayRect = rect;
                rect.set(position.x, 0, mColRect.width, position.y);
                float tmpGroundY = 0;
                float distance = mColRect.y;
                GameObject closestObject = null;
                //for(GameObject go : objects)
                for (int i = 0; i < objects.size; i++)
                {
                    GameObject go = objects.get(i);
                    if (go == null) continue;
                    if (go instanceof Sprite
                            && (((Sprite) go).type == Sprite.Type.massive || ((Sprite) go).type == Sprite.Type.halfmassive)
                            && rect.overlaps(go.mColRect))
                    {
                        if (((Sprite) go).type == Sprite.Type.halfmassive && mColRect.y < go.mColRect.y + go.mColRect.height)
                        {
                            continue;
                        }
                        float tmpDistance = mColRect.y - (go.mColRect.y + go.mColRect.height);
                        if (tmpDistance < distance)
                        {
                            distance = tmpDistance;
                            tmpGroundY = go.mColRect.y + go.mColRect.height;
                            closestObject = go;
                        }
                    }
                }
                groundY = tmpGroundY;
                if (closestObject != null
                        && closestObject instanceof Sprite
                        && ((Sprite) closestObject).type == Sprite.Type.halfmassive
                        && worldState == WorldState.DUCKING)
                {
                    position.y -= 0.1f;
                }
                World.RECT_POOL.free(rect);
            }
        }
        if (powerJump)
        {
            powerJumpEffect.update(delta);
        }
        bulletShotTime += delta;
    }

    @Override
    protected boolean handleCollision(GameObject object, boolean vertical)
    {
        if (!handleCollision) return false;
        super.handleCollision(object, vertical);
        if (object instanceof Item)
        {
            Item item = (Item) object;
            if (!item.playerHit) item.hitPlayer();
            world.trashObjects.add(item);
        }
        else if (object instanceof Enemy && ((Enemy) object).handleCollision)
        {
            if (!godMode)
            {
                boolean deadAnyway = isDeadByJumpingOnTopOfEnemy(object);
                if (((Enemy) object).frozen)
                {
                    ((Enemy) object).downgradeOrDie(this, true);
                }
                else if (deadAnyway)
                {
                    downgradeOrDie(false);
                }
                else
                {
                    int resolution = ((Enemy) object).hitByPlayer(this, vertical);
                    if (resolution == Enemy.HIT_RESOLUTION_ENEMY_DIED)
                    {
                        velocity.y = 5f * Gdx.graphics.getDeltaTime();
                    }
                    else if (resolution == Enemy.HIT_RESOLUTION_PLAYER_DIED)
                    {
                        downgradeOrDie(false);
                    }
                    else
                    {
                        //TODO handle this here or in enemy???????
                    }
                }
            }
        }
        else if (object instanceof Box && position.y + mColRect.height <= object.position.y)
        {
            ((Box) object).handleHitByPlayer();
        }
        return false;
    }

    private boolean isDeadByJumpingOnTopOfEnemy(GameObject object)
    {
        //TODO update this when you add new enemy classes
        return object instanceof Flyon || object instanceof Eato || object instanceof Thromp
                || object instanceof Spika;
    }

    public WorldState getWorldState()
    {
        return worldState;
    }

    public void setWorldState(WorldState newWorldState)
    {
        if (worldState == WorldState.DYING) return;
        this.worldState = newWorldState;
        if (worldState == WorldState.DUCKING)
        {
            mColRect.height = mDrawRect.height / 2;
        }
        else
        {
            mColRect.height = mDrawRect.height * 0.9f;
        }
        if (worldState == WorldState.CLIMBING)
        {
            exitEnterStartPosition.set(position);
            velocity.x = 0;
            velocity.y = 0;
        }
    }

    @Override
    public float maxVelocity()
    {
        return MAX_VEL;
    }

    public void downgradeOrDie(boolean forceDie)
    {
        if (maryoState == MaryoState.small || forceDie)
        {
            worldState = WorldState.DYING;
            dyingAnim.start();
        }
        else
        {
            godMode = true;
            godModeActivatedTime = System.currentTimeMillis();

            if (((GameScreen) world.screen).hud.item != null)
            {
                //drop item
                Item item = ((GameScreen) world.screen).hud.item;
                OrthographicCamera cam = ((GameScreen) world.screen).cam;

                item.mColRect.x = item.position.x = cam.position.x - item.mColRect.width * 0.5f;
                item.mColRect.y = item.position.y = cam.position.y + cam.viewportHeight * 0.5f - 1.5f;

                item.updateBounds();

                world.level.gameObjects.add(item);
                item.drop();

                ((GameScreen) world.screen).hud.item = null;
            }

            maryoState = MaryoState.small;
            GameSaveUtility.getInstance().save.playerState = maryoState;
            setupBoundingBox();
        }
    }

    /*
    * Level up*/
    public void upgrade(MaryoState newState, boolean tempUpdate, Item item)
    {
        //cant upgrade from ice/fire to big
        if ((maryoState == newState && (newState == MaryoState.big || newState == MaryoState.ice || newState == MaryoState.fire))
                || (newState == MaryoState.big && (maryoState == MaryoState.ice || maryoState == MaryoState.fire)))
        {
            ((GameScreen) world.screen).hud.item = item;
            return;
        }
        else if (maryoState == newState)
        {
            return;
        }
        this.newState = newState;
        oldState = maryoState;
        Array<TextureRegion> frames = generateResizeAnimationFrames(maryoState, newState);
        resizingAnimation = new Animation(RESIZE_ANIMATION_FRAME_DURATION, frames);
        resizingAnimation.setPlayMode(Animation.PlayMode.LOOP);
        resizeAnimStartTime = stateTime;
        godMode = true;

        ((GameScreen) world.screen).setGameState(GameScreen.GAME_STATE.PLAYER_UPDATING);

        //play new state sound
        Sound sound = upgradeSound(newState);
        if (sound != null && Assets.playSounds) sound.play();
    }

    private Sound upgradeSound(MaryoState newState)
    {
        switch (newState)
        {
            case big:
                return Assets.manager.get("data/sounds/item/mushroom.ogg");
            case fire:
                return Assets.manager.get("data/sounds/item/fireplant.ogg");
            case ice:
                return Assets.manager.get("data/sounds/item/mushroom_blue.wav");
            case ghost:
                return Assets.manager.get("data/sounds/item/mushroom_ghost.ogg");
            case flying:
                //TODO this asset is missing somehow
                //return Assets.manager.get("data/sounds/item/feather.ogg");
        }
        return null;
    }

    private Array<TextureRegion> generateResizeAnimationFrames(MaryoState stateFrom, MaryoState stateTo)
    {
        Array<TextureRegion> regions = new Array<>();
        if (worldState.equals(WorldState.WALKING))
        {
            regions.add(aMap[aIndex(stateFrom, AKey.walk)].getKeyFrame(stateTime, true));
            regions.add(aMap[aIndex(stateTo, AKey.walk)].getKeyFrame(stateTime, true));
        }
        else if (worldState == WorldState.DUCKING)
        {
            regions.add(tMap[tIndex(stateFrom, TKey.duck_right)]);
            regions.add(tMap[tIndex(stateTo, TKey.duck_right)]);
        }
        else if (getWorldState().equals(WorldState.JUMPING))
        {
            if (velocity.y > 0)
            {
                regions.add(tMap[tIndex(stateFrom, TKey.jump_right)]);
                regions.add(tMap[tIndex(stateTo, TKey.jump_right)]);
            }
            else
            {
                regions.add(tMap[tIndex(stateFrom, TKey.fall_right)]);
                regions.add(tMap[tIndex(stateTo, TKey.fall_right)]);
            }
        }
        else if (worldState == WorldState.DYING)
        {
            regions.add(tMap[tIndex(stateFrom, TKey.dead_right)]);
            regions.add(tMap[tIndex(stateTo, TKey.dead_right)]);
        }
        else
        {
            regions.add(tMap[tIndex(stateFrom, TKey.stand_right)]);
            regions.add(tMap[tIndex(stateTo, TKey.stand_right)]);
        }
        return regions;
    }

    public class DyingAnimation
    {
        private float diedTime;
        boolean upAnimFinished, dyedReset, firstDelayFinished;
        Vector3 diedPosition;
        boolean upBoost;

        public void start()
        {
            diedTime = stateTime;
            handleCollision = false;
            diedPosition = new Vector3(position);
            if (Assets.playSounds)
            {
                Sound sound = Assets.manager.get("data/sounds/player/dead.ogg");
                sound.play();
            }
            ((GameScreen) world.screen).setGameState(GameScreen.GAME_STATE.PLAYER_DEAD);
            GameSaveUtility.getInstance().save.lifes--;
        }

        public boolean update(float delat)
        {
            velocity.x = 0;
            position.x = diedPosition.x;
            if (mDrawRect.y + mDrawRect.height < 0)//first check if player is visible
            {
                ((GameScreen) world.screen).setGameState(GameScreen.GAME_STATE.GAME_OVER);
                world.trashObjects.add(Maryo.this);
                return false;
            }

            if (!firstDelayFinished && stateTime - diedTime < 0.5f)//delay 500ms
            {
                return false;
            }
            else
            {
                firstDelayFinished = true;
            }

            if (!upBoost)
            {
                //animate player up a bit
                velocity.y = 8f;
                upBoost = true;
            }

            return true;
        }
    }

    @Override
    protected boolean handleDroppedBelowWorld()
    {
        if (worldState != WorldState.DYING)
        {
            downgradeOrDie(true);
        }
        return true;
    }

    private void setJumpSound()
    {
        switch (maryoState)
        {
            case small:
                jumpSound = Assets.manager.get("data/sounds/player/jump_small.ogg");
                break;
            case big:
            case fire:
            case ice:
                jumpSound = Assets.manager.get("data/sounds/player/jump_big.ogg");
                break;
            case ghost:
                jumpSound = Assets.manager.get("data/sounds/player/jump_ghost.ogg");
                break;
        }
    }

    public MaryoState getMarioState()
    {
        return maryoState;
    }

    public void setMarioState(MaryoState marioState)
    {
        this.maryoState = marioState;
        setJumpSound();
    }

    public void checkLevelEnter()
    {
        //check if maryo is overlapping level entry and if so call enterLevel
        for (GameObject go : world.level.gameObjects)
        {
            if (go instanceof LevelEntry && mColRect.overlaps(go.mColRect) && ((LevelEntry) go).type == LevelExit.LEVEL_EXIT_WARP)
            {
                LevelEntry entry = (LevelEntry) go;
                if (entry.type == LevelExit.LEVEL_EXIT_BEAM)
                {
                    float entryCenter = entry.mColRect.x + entry.mColRect.width * 0.5f;
                    position.x = mColRect.x = entryCenter - mColRect.width * 0.5f;
                    position.y = mColRect.y = entry.mColRect.y + entry.mColRect.height + mColRect.height;
                    updateBounds();
                    return;
                }
                else
                {
                    enterLevel((LevelEntry) go);
                    return;
                }
            }
        }
    }

    public void enterLevel(LevelEntry entry)
    {
        ((GameScreen) world.screen).setGameState(GameScreen.GAME_STATE.PLAYER_UPDATING);
        entering = true;
        this.entry = entry;
        if (entry.type == LevelExit.LEVEL_EXIT_WARP)
        {
            // left
            if ("left".equals(entry.direction))
            {
                position.x = mColRect.x = entry.mColRect.x + entry.mColRect.width;

                float entryCenter = entry.mColRect.y + entry.mColRect.height * 0.5f;
                position.y = mColRect.y = entryCenter - mColRect.height * 0.5f;
            }
            // right
            else if ("right".equals(entry.direction))
            {
                position.x = mColRect.x = entry.mColRect.x - mColRect.width;


                float entryCenter = entry.mColRect.y + entry.mColRect.height * 0.5f;
                position.y = mColRect.y = entryCenter - mColRect.height * 0.5f;
            }
            //up
            else if ("up".equals(entry.direction))
            {
                position.y = mColRect.y = entry.mColRect.y - mColRect.height;

                float entryCenter = entry.mColRect.x + entry.mColRect.width * 0.5f;
                position.x = mColRect.x = entryCenter - mColRect.width * 0.5f;
            }
            // down
            else if ("down".equals(entry.direction))
            {
                position.y = mColRect.y = entry.mColRect.y;

                float entryCenter = entry.mColRect.x + entry.mColRect.width * 0.5f;
                position.x = mColRect.x = entryCenter - mColRect.width * 0.5f;
            }
        }
        else if (entry.type == LevelExit.LEVEL_EXIT_BEAM)
        {
            float entryCenter = entry.mColRect.x + entry.mColRect.width * 0.5f;
            position.x = mColRect.x = entryCenter - mColRect.width * 0.5f;
            position.y = mColRect.y = entry.mColRect.y + entry.mColRect.height + mColRect.height;
        }
        updateBounds();
        exitEnterStartPosition.set(position);
        position.z = LevelLoader.m_pos_z_passive_start;
        Collections.sort(world.level.gameObjects, new LevelLoader.ZSpriteComparator());

        //todo sound
    }

    public void exitLevel(LevelExit exit)
    {
        System.out.println("level: " + exit.levelName);

        switch (exit.type)
        {
            case LevelExit.LEVEL_EXIT_BEAM:
                //just change level
                String nextLevelName;
                if (exit.levelName == null)
                {
                    String currentLevel = ((GameScreen) world.screen).parent == null ? ((GameScreen) world.screen).levelName : ((GameScreen) world.screen).parent.levelName;
                    nextLevelName = GameSaveUtility.getInstance().getNextLevel(currentLevel);
                }
                else
                {
                    nextLevelName = exit.levelName;
                }
                world.screen.game.setScreen(new LoadingScreen(new GameScreen(world.screen.game, false, nextLevelName), false));
                break;
            case LevelExit.LEVEL_EXIT_WARP:
                if (exiting) return;
                ((GameScreen) world.screen).setGameState(GameScreen.GAME_STATE.PLAYER_UPDATING);
                exiting = true;
                this.exit = exit;
                if ("up".equals(exit.direction) || "down".equals(exit.direction))
                {
                    float exitCenter = exit.mColRect.x + exit.mColRect.width * 0.5f;
                    position.x = mColRect.x = exitCenter - mColRect.width * 0.5f;
                }
                else
                {
                    float exitCenter = exit.mColRect.y + exit.mColRect.height * 0.5f;
                    position.y = mColRect.y = exitCenter - mColRect.height * 0.5f;
                }
                updateBounds();
                exitEnterStartPosition.set(position);
                position.z = LevelLoader.m_pos_z_passive_start;
                Collections.sort(world.level.gameObjects, new LevelLoader.ZSpriteComparator());

                //todo sound
                break;
        }
    }

    public void fire()
    {
        if (worldState == WorldState.DUCKING)
            return;
        if (bulletShotTime < BULLET_COOLDOWN)
            return;
        fire = true;
        doFire();
    }

    private void doFire()
    {
        if (maryoState == MaryoState.fire)
        {
            Fireball fireball = world.FIREBALL_POOL.obtain();
            fireball.mColRect.x = fireball.position.x = mDrawRect.x + mDrawRect.width * 0.5f;
            fireball.mColRect.y = fireball.position.y = mDrawRect.y + mDrawRect.height * 0.5f;
            fireball.updateBounds();
            fireball.reset();
            fireball.direction = facingLeft ? Direction.left : Direction.right;
            world.level.gameObjects.add(fireball);
            Collections.sort(world.level.gameObjects, new LevelLoader.ZSpriteComparator());
            bulletShotTime = 0;
        }
        else if (maryoState == MaryoState.ice)
        {
            Iceball iceball = world.ICEBALL_POOL.obtain();
            iceball.mColRect.x = iceball.position.x = mDrawRect.x + mDrawRect.width * 0.5f;
            iceball.mColRect.y = iceball.position.y = mDrawRect.y + mDrawRect.height * 0.5f;
            iceball.updateBounds();
            iceball.reset();
            iceball.direction = facingLeft ? Direction.left : Direction.right;
            world.level.gameObjects.add(iceball);
            Collections.sort(world.level.gameObjects, new LevelLoader.ZSpriteComparator());
            bulletShotTime = 0;
        }
    }
}
