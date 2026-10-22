package com.blamejared.primalchests.dev;

import com.blamejared.primalchests.PRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import static com.blamejared.primalchests.reference.Reference.MOD_ID;

/**
 * Development only (excluded from the release jar, PrimalChests needs no JEI plugin): grabs the JEI runtime so the
 * visual test can show the recipes of both chests.
 */
@JeiPlugin
public final class JeiShots implements IModPlugin {

    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(MOD_ID, "dev_screenshots");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    static void show(int step) {
        if(runtime == null) {
            return;
        }
        ItemStack stack = new ItemStack(step == 0 ? PRegistry.PRIMAL_CHEST_ITEM.get() : PRegistry.PRIMAL_CHEST_ADVANCED_ITEM.get());
        runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
    }
}
