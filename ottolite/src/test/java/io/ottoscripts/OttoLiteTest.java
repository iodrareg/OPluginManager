package io.ottoscripts;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.PacketUtils.PacketUtilsPlugin;
import io.ottoscripts.agility.OttoAgilityPlugin;
import io.ottoscripts.gauntlet.OttoGauntletPlugin;
import io.ottoscripts.scurrius.OttoScurriusPlugin;
import io.ottoscripts.thieving.OttoThievingPlugin;
import io.ottoscripts.usermanagement.OttoUserManagementPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OttoLiteTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(
            EthanApiPlugin.class,
            PacketUtilsPlugin.class,
            OttoThievingPlugin.class,
            OttoScurriusPlugin.class,
            OttoAgilityPlugin.class,
            OttoGauntletPlugin.class,
            OttoLitePlugin.class,
            OttoUserManagementPlugin.class
        );
        RuneLite.main(args);
    }
}
