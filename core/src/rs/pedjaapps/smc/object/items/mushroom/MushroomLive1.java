package rs.pedjaapps.smc.object.items.mushroom;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import rs.pedjaapps.smc.audio.SoundManager;
import rs.pedjaapps.smc.object.World;
import rs.pedjaapps.smc.utility.GameSave;

/**
 * Created by pedja on 29.3.15..
 * <p/>
 * This file is part of SMC-Android
 * Copyright Predrag Čokulov 2015
 */
public class MushroomLive1 extends Mushroom
{
    public MushroomLive1(World world, Vector2 size, Vector3 position)
    {
        super(world, size, position);
        textureName = "data/game/items/mushroom_green.png";
        mPickPoints = 1000;
    }

    @Override
    protected void performCollisionAction()
    {
        playerHit = true;
        GameSave.save.lifes += 1;
        Sound sound = world.screen.game.assets.manager.get("data/sounds/item/live_up.ogg");
        SoundManager.play(sound);
        world.trashObjects.add(this);
        GameSave.save.points += mPickPoints;
    }
}
