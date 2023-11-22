package com.mrbysco.nbt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrbysco.nbt.NotableBubbleText;
import com.mrbysco.nbt.client.util.BubbleRenderer;
import com.mrbysco.nbt.command.BubbleText;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.UUID;

public class ClientHandler {

	@SubscribeEvent
	public <T extends LivingEntity> void onEntityRender(RenderLivingEvent.Post<T, ? extends EntityModel<T>> event) {
		final Minecraft mc = Minecraft.getInstance();
		final Player localPlayer = mc.player;
		if (localPlayer == null) return;

		final LivingEntity livingEntity = event.getEntity();
		if (livingEntity.isInvisibleTo(localPlayer)) return;

		String author = BubbleHandler.getAuthor(livingEntity.getUUID());
		if (!author.isEmpty()) {
			List<BubbleText> bubbles = BubbleHandler.getBubbles(author);
			if (bubbles.isEmpty()) return;
			BubbleText bubble = bubbles.get(0);

			final Level level = mc.level;
			if (level == null) return;
			final long currentTime = level.getGameTime();

			long bubbleTime = bubbles.size() > 1 ? 50 : 200;
			long bubbleAge = bubble.getAge(currentTime);
			float bubbleAlpha = bubble.getAlpha(currentTime);

			final Font font = mc.font;
			final PoseStack poseStack = event.getPoseStack();
			final EntityDimensions dimensions = livingEntity.getDimensions(livingEntity.getPose());
			final MultiBufferSource multiBufferSource = event.getMultiBufferSource();

			BubbleRenderer.renderBubbleText(bubble, poseStack, font, multiBufferSource, mc.getEntityRenderDispatcher(),
					dimensions.height, bubbleAlpha, event.getPackedLight());

			if (bubbleAge > bubbleTime) {
				BubbleHandler.removeBubble(bubble);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerRender(RenderPlayerEvent.Post event) {
		if (!ConfigCache.renderPlayerBubbles) return;
		final Minecraft mc = Minecraft.getInstance();
		final Player localPlayer = mc.player;
		if (localPlayer == null) return;

		final Player player = event.getPlayer();
		if (player.isInvisibleTo(localPlayer)) return;

		List<BubbleText> bubbles = BubbleHandler.getPlayerBubbles(player.getUUID());
		if (!bubbles.isEmpty()) {
			BubbleText bubble = bubbles.get(0);

			final Level level = mc.level;
			if (level == null) return;
			final long currentTime = level.getGameTime();

			long bubbleTime = bubbles.size() > 1 ? 50 : 200;
			long bubbleAge = bubble.getAge(currentTime);
			float bubbleAlpha = bubble.getAlpha(currentTime);

			final Font font = mc.font;
			final PoseStack poseStack = event.getPoseStack();
			final EntityDimensions dimensions = player.getDimensions(player.getPose());
			final MultiBufferSource multiBufferSource = event.getMultiBufferSource();

			BubbleRenderer.renderBubbleText(bubble, poseStack, font, multiBufferSource, mc.getEntityRenderDispatcher(),
					dimensions.height, bubbleAlpha, event.getPackedLight());

			if (bubbleAge > bubbleTime) {
				BubbleHandler.removePlayerBubble(bubble);
			}
		}
	}


	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerReceiveChat(ClientChatReceivedEvent event) {
		final UUID sender = event.getSenderUUID();
		if (sender == null || sender == Util.NIL_UUID || !ConfigCache.renderPlayerBubbles) return;

		final Component message = event.getMessage();
		final Minecraft mc = Minecraft.getInstance();
		final Player player = mc.player;
		if (player == null) return;
		final Level level = mc.level;
		if (level == null) return;

		Player senderPlayer = level.getPlayerByUUID(sender);
		if (senderPlayer == null) return;

		if (player.blockPosition().distManhattan(senderPlayer.blockPosition()) < 2000 || player.level.dimension().location().equals(senderPlayer.level.dimension().location())) {
			String senderName = senderPlayer.getGameProfile().getName();
			String messageText = message.getString();
			if (!BubbleHandler.addPlayerBubble(sender, new BubbleText(senderName, messageText, sender, level.getGameTime()))) {
				NotableBubbleText.LOGGER.error("Failed to add player bubble for {}", senderName);
			}
		}
	}
}
