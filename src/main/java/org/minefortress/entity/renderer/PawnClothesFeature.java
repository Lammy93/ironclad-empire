package org.minefortress.entity.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.minefortress.entity.BasePawnEntity;
import org.minefortress.entity.renderer.models.PawnModel;

public class PawnClothesFeature extends FeatureRenderer<BasePawnEntity, PawnModel> {

    private static final Identifier ARCHER = Identifier.of("minefortress", "textures/skins/archer.png");
    private static final Identifier ARMORER = Identifier.of("minefortress", "textures/skins/armorer.png");
    private static final Identifier BAKER = Identifier.of("minefortress", "textures/skins/baker.png");
    private static final Identifier BLACKSMITH = Identifier.of("minefortress", "textures/skins/blacksmith.png");
    private static final Identifier BUTCHER = Identifier.of("minefortress", "textures/skins/butcher.png");
    private static final Identifier COLONIST = Identifier.of("minefortress", "textures/skins/colonist.png");
    private static final Identifier COOK = Identifier.of("minefortress", "textures/skins/cook.png");
    private static final Identifier CRAFTER = Identifier.of("minefortress", "textures/skins/crafter.png");
    private static final Identifier CROSSBOWMAN = Identifier.of("minefortress", "textures/skins/crossbowman.png");
    private static final Identifier FARMER = Identifier.of("minefortress", "textures/skins/farmer.png");
    private static final Identifier FISHERMAN = Identifier.of("minefortress", "textures/skins/fisherman.png");
    private static final Identifier FOOTMAN = Identifier.of("minefortress", "textures/skins/footman.png");
    private static final Identifier FORESTER = Identifier.of("minefortress", "textures/skins/forester.png");
    private static final Identifier HUNTER = Identifier.of("minefortress", "textures/skins/hunter.png");
    private static final Identifier KNIGHT = Identifier.of("minefortress", "textures/skins/knight.png");
    private static final Identifier LEATHER_WORKER = Identifier.of("minefortress", "textures/skins/leather_worker.png");
    private static final Identifier LUMBERJACK = Identifier.of("minefortress", "textures/skins/lumberjack.png");
    private static final Identifier LUMBERJACK_T2 = Identifier.of("minefortress", "textures/skins/lumberjack_t2.png");
    private static final Identifier LUMBERJACK_T3 = Identifier.of("minefortress", "textures/skins/lumberjack_t3.png");
    private static final Identifier MINER = Identifier.of("minefortress", "textures/skins/miner.png");
    private static final Identifier MINER_T2 = Identifier.of("minefortress", "textures/skins/miner_t2.png");
    private static final Identifier MINER_T3 = Identifier.of("minefortress", "textures/skins/miner_t3.png");
    private static final Identifier RIDER = Identifier.of("minefortress", "textures/skins/rider.png");
    private static final Identifier SADDLER = Identifier.of("minefortress", "textures/skins/saddler.png");
    private static final Identifier SHEPHERD = Identifier.of("minefortress", "textures/skins/shepherd.png");
    private static final Identifier STABLEMAN = Identifier.of("minefortress", "textures/skins/stableman.png");
    private static final Identifier TAILOR = Identifier.of("minefortress", "textures/skins/tailor.png");
    private static final Identifier WARRIOR = Identifier.of("minefortress", "textures/skins/warrior.png");
    private static final Identifier WEAVER = Identifier.of("minefortress", "textures/skins/weaver.png");

    // soldier clothes
    private static final Identifier SOLDIER_BLACK = Identifier.of("minefortress", "textures/skins/soldier/soldier_black.png");
    private static final Identifier SOLDIER_BLUE = Identifier.of("minefortress", "textures/skins/soldier/soldier_blue.png");
    private static final Identifier SOLDIER_BROWN = Identifier.of("minefortress", "textures/skins/soldier/soldier_brown.png");
    private static final Identifier SOLDIER_CYAN = Identifier.of("minefortress", "textures/skins/soldier/soldier_cyan.png");
    private static final Identifier SOLDIER_GRAY = Identifier.of("minefortress", "textures/skins/soldier/soldier_gray.png");
    private static final Identifier SOLDIER_GREEN = Identifier.of("minefortress", "textures/skins/soldier/soldier_green.png");
    private static final Identifier SOLDIER_LIGHT_BLUE = Identifier.of("minefortress", "textures/skins/soldier/soldier_lightblue.png");
    private static final Identifier SOLDIER_LIGHT_GRAY = Identifier.of("minefortress", "textures/skins/soldier/soldier_lightgray.png");
    private static final Identifier SOLDIER_LIME = Identifier.of("minefortress", "textures/skins/soldier/soldier_lime.png");
    private static final Identifier SOLDIER_MAGENTA = Identifier.of("minefortress", "textures/skins/soldier/soldier_magenta.png");
    private static final Identifier SOLDIER_PURPLE = Identifier.of("minefortress", "textures/skins/soldier/soldier_purple.png");
    private static final Identifier SOLDIER_RED = Identifier.of("minefortress", "textures/skins/soldier/soldier_red.png");
    private static final Identifier SOLDIER_WHITE = Identifier.of("minefortress", "textures/skins/soldier/soldier_white.png");
    private static final Identifier SOLDIER_YELLOW = Identifier.of("minefortress", "textures/skins/soldier/soldier_yellow.png");

    public PawnClothesFeature(
            FeatureRendererContext<BasePawnEntity, PawnModel> context
    ) {
        super(context);
    }

    public static Identifier getClothesTexture(String clothingId) {
        return switch (clothingId) {
            case "miner1" -> MINER;
            case "miner2" -> MINER_T2;
            case "miner3" -> MINER_T3;
            case "lumberjack1" -> LUMBERJACK;
            case "lumberjack2" -> LUMBERJACK_T2;
            case "lumberjack3" -> LUMBERJACK_T3;
            case "forester" -> FORESTER;
            case "hunter" -> HUNTER;
            case "fisherman" -> FISHERMAN;
            case "farmer" -> FARMER;
            case "baker" -> BAKER;
            case "shepherd" -> SHEPHERD;
            case "stableman" -> STABLEMAN;
            case "butcher" -> BUTCHER;
            case "cook" -> COOK;
            case "crafter" -> CRAFTER;
            case "leather_worker1", "leather_worker2" -> LEATHER_WORKER;
            case "blacksmith" -> BLACKSMITH;
            case "weaver" -> WEAVER;
            case "warrior1" -> WARRIOR;
            case "warrior2" -> FOOTMAN;
            case "archer1" -> ARCHER;
            default -> COLONIST;
        };
    }

    protected static <T extends LivingEntity> void renderModel(EntityModel<T> model, Identifier texture, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float red, float green, float blue) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
        model.render(matrices, vertexConsumer, light, LivingEntityRenderer.getOverlay(entity, 0.0f),
                net.minecraft.util.math.ColorHelper.Argb.getArgb(255, (int) (red * 255), (int) (green * 255), (int) (blue * 255))); // 1.21+: packed ARGB
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BasePawnEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if(entity.isSleeping()) return;
        final var clothingId = entity.getClothingId();
        if(isWarrior(clothingId)) {
            PawnClothesFeature.renderModel(this.getContextModel(), SOLDIER_BLUE, matrices, vertexConsumers, light, entity, 1.0f, 1.0f, 1.0f);
        }
        PawnClothesFeature.renderModel(this.getContextModel(), getClothesTexture(clothingId), matrices, vertexConsumers, light, entity, 1.0f, 1.0f, 1.0f);

    }

    private boolean isWarrior(String professionId) {
        return professionId.equals("warrior1") || professionId.equals("warrior2") || professionId.startsWith("archer");
    }
}
