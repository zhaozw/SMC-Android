package rs.pedjaapps.smc.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;

import rs.pedjaapps.smc.utility.PrefsManager;

public class MaryoFileHandleResolver implements FileHandleResolver
{
    private static final int[] ASSETS_RESOLUTIONS = new int[]{1080, 768, 540};

    public static int DEFAULT_TEXTURE_QUALITY = -1;

    private int resolution;

    MaryoFileHandleResolver()
    {
        int prefResolution = PrefsManager.getTextureQuality();

        if(prefResolution >= 0 && prefResolution < ASSETS_RESOLUTIONS.length)
        {
            resolution = ASSETS_RESOLUTIONS[ASSETS_RESOLUTIONS.length - 1 - prefResolution];
        }
        else
        {
            int height = Gdx.graphics.getHeight();

            int tmpRes = 768;
            int diff = Integer.MAX_VALUE;
            int offset = ASSETS_RESOLUTIONS.length;
            for (int res : ASSETS_RESOLUTIONS)
            {
                int tmp;
                if ((tmp = Math.abs(res - height)) < diff)
                {
                    diff = tmp;
                    tmpRes = res;
                    offset--;
                }
            }
            resolution = tmpRes;
            DEFAULT_TEXTURE_QUALITY = offset;
            PrefsManager.setTextureQuality(DEFAULT_TEXTURE_QUALITY);
        }
    }

    @Override
    public FileHandle resolve(String fileName)
    {
        fileName = getFileNameForResolution(fileName);
        return Gdx.files.internal(fileName);
    }

    private String getFileNameForResolution(String fileName)
    {
        String newFileName = fileName.replaceFirst("data", "data_" + resolution);
        FileHandle handle = Gdx.files.internal(newFileName);
        if(handle.exists())
        {
            return newFileName;
        }
        else
        {
            return fileName;
        }
    }


}