package com.mrbysco.nbt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrbysco.nbt.NotableBubbleText;
import com.mrbysco.nbt.client.util.BubbleRenderer;
import com.mrbysco.nbt.command.BubbleText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ClientHandler {

	@SubscribeEvent
	public <T extends LivingEntity> void onEntityRender(RenderLivingEvent.Post<T, ? extends EntityModel<T>> event) {
		final float partialTick = event.getPartialTick();
		final Minecraft mc = Minecraft.getInstance();
		final Player localPlayer = mc.player;
		if (localPlayer == null) return;

		final LivingEntity livingEntity = event.getEntity();
		if (livingEntity.isInvisibleTo(localPlayer)) return;

		String author = BubbleHandler.getAuthor(livingEntity.getUUID());
		if (!author.isEmpty()) {
			List<BubbleText> bubbles = BubbleHandler.getBubbles(author);
			if (bubbles.isEmpty()) return;
			BubbleText bubble = bubbles.getFirst();

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
			final EntityRenderDispatcher renderDispatcher = mc.getEntityRenderDispatcher();
			final double nameOffset = getNameOffset(renderDispatcher, livingEntity, partialTick);

			BubbleRenderer.renderBubbleText(bubble, poseStack, font, multiBufferSource, renderDispatcher,
					dimensions.height(), bubbleAlpha, event.getPackedLight(), nameOffset);

			if (bubbleAge > bubbleTime) {
				BubbleHandler.removeBubble(bubble);
			}
		}
	}

	public static double getNameOffset(EntityRenderDispatcher renderDispatcher, LivingEntity livingEntity, float partialTick) {
		double nameOffset = 0.0D;
		if (!ConfigCache.nameOffset) {
			return nameOffset;
		}

		boolean flag = livingEntity.shouldShowName() || (livingEntity == renderDispatcher.crosshairPickEntity && livingEntity.hasCustomName());
		if (!flag) return nameOffset;

		Vec3 vec3 = livingEntity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, livingEntity.getViewYRot(partialTick));
		if (flag && vec3 != null) {
			nameOffset += vec3.y * 0.3125D;
		}

		return nameOffset;
	}

	@SubscribeEvent
	public void onPlayerRender(RenderPlayerEvent.Post event) {
		if (!ConfigCache.renderPlayerBubbles) return;
		final Minecraft mc = Minecraft.getInstance();
		final Player localPlayer = mc.player;
		if (localPlayer == null) return;

		final Player player = event.getEntity();
		if (player.isInvisibleTo(localPlayer)) return;
		final float partialTick = event.getPartialTick();

		List<BubbleText> bubbles = BubbleHandler.getPlayerBubbles(player.getUUID());
		if (!bubbles.isEmpty()) {
			BubbleText bubble = bubbles.getFirst();

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
			final EntityRenderDispatcher renderDispatcher = mc.getEntityRenderDispatcher();
			final double nameOffset = getNameOffset(renderDispatcher, player, partialTick);

			BubbleRenderer.renderBubbleText(bubble, poseStack, font, multiBufferSource, renderDispatcher,
					dimensions.height(), bubbleAlpha, event.getPackedLight(), nameOffset);

			if (bubbleAge > bubbleTime) {
				BubbleHandler.removePlayerBubble(bubble);
			}
		}
	}


	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerReceiveChat(ClientChatReceivedEvent.Player event) {
		if (!ConfigCache.renderPlayerBubbles) return;

		final UUID sender = event.getSender();
		final Component message = event.getMessage();
		final Minecraft mc = Minecraft.getInstance();
		final Player player = mc.player;
		if (player == null) return;
		final Level level = mc.level;
		if (level == null) return;

		Player senderPlayer = level.getPlayerByUUID(sender);
		if (senderPlayer == null) return;

		if (player.blockPosition().distManhattan(senderPlayer.blockPosition()) < 2000 || player.level().dimension().location().equals(senderPlayer.level().dimension().location())) {
			String senderName = senderPlayer.getGameProfile().getName();
			String messageText = message.getString();
			List<Component> siblings = message.getSiblings();
			if (!siblings.isEmpty()) {
				String lastText = siblings.getLast().getString();
				if (!lastText.isEmpty())
					messageText = lastText;
			}

			boolean showUsername = ConfigCache.showUsername;
			if (!showUsername && message.getContents() instanceof TranslatableContents translatableContents) {
				final Object[] args = translatableContents.getArgs();
				if (args.length > 1) {
					final Object[] adjustedArgs = Arrays.copyOfRange(args, 1, args.length);
					StringBuilder justTheMessage = new StringBuilder();
					for (Object adjustedArg : adjustedArgs) {
						if (adjustedArg instanceof Component component) {
							justTheMessage.append(component.getString());
						}
						if (adjustedArg instanceof String str) {
							justTheMessage.append(str);
						}
					}
					messageText = justTheMessage.toString();
				}
			}
			if (!BubbleHandler.addPlayerBubble(sender, new BubbleText(senderName, messageText, sender, level.getGameTime()))) {
				NotableBubbleText.LOGGER.error("Failed to add player bubble for {}", senderName);
			}
		}
	}
}
