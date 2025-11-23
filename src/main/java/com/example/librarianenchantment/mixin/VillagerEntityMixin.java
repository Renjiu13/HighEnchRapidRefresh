package com.example.librarianenchantment.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(VillagerEntity.class)
public class VillagerEntityMixin {
    @Inject(method = "fillRecipes", at = @At("TAIL"))
    private void enhanceLibrarianTrades(CallbackInfoReturnable<TradeOfferList> cir) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getVillagerData();
        
        // 检查是否为图书管理员职业
        if (data.getProfession() == VillagerProfession.LIBRARIAN) {
            TradeOfferList trades = cir.getReturnValue();
            
            // 增强图书管理员的附魔书交易
            enhanceEnchantmentTrades(trades, villager.getRandom());
        }
    }
    
    private void enhanceEnchantmentTrades(TradeOfferList trades, Random random) {
        // 定义高级附魔及其最大等级
        Map<Enchantment, Integer> highLevelEnchantments = new HashMap<>();
        highLevelEnchantments.put(Enchantments.PROTECTION, 4); // 最大保护IV
        highLevelEnchantments.put(Enchantments.SHARPNESS, 5); // 最大锋利V
        highLevelEnchantments.put(Enchantments.EFFICIENCY, 5); // 最大效率V
        highLevelEnchantments.put(Enchantments.FORTUNE, 3); // 最大时运III
        highLevelEnchantments.put(Enchantments.LOOTING, 3); // 最大抢夺III
        highLevelEnchantments.put(Enchantments.UNBREAKING, 3); // 最大耐久III
        highLevelEnchantments.put(Enchantments.POWER, 5); // 最大力量V
        
        // 遍历所有交易，寻找附魔书交易并增强它们
        for (int i = 0; i < trades.size(); i++) {
            TradeOffer trade = trades.get(i);
            ItemStack sellItem = trade.getSellItem();
            
            // 检查是否为附魔书
            if (sellItem.getItem() == Items.ENCHANTED_BOOK) {
                // 获取附魔书上的附魔
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(sellItem);
                
                // 如果有附魔，则尝试增强它们
                if (!enchantments.isEmpty()) {
                    Map<Enchantment, Integer> enhancedEnchantments = new HashMap<>();
                    
                    for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                        Enchantment enchantment = entry.getKey();
                        int currentLevel = entry.getValue();
                        
                        // 获取该附魔的最大等级
                        int maxLevel = highLevelEnchantments.getOrDefault(enchantment, enchantment.getMaxLevel());
                        
                        // 提高等级（但不超过最大值）
                        int enhancedLevel = Math.min(currentLevel + 1, maxLevel);
                        
                        enhancedEnchantments.put(enchantment, enhancedLevel);
                    }
                    
                    // 创建新的增强附魔书
                    ItemStack enhancedBook = new ItemStack(Items.ENCHANTED_BOOK);
                    for (Map.Entry<Enchantment, Integer> entry : enhancedEnchantments.entrySet()) {
                        EnchantedBookItem.addEnchantment(enhancedBook, 
                            new net.minecraft.enchantment.EnchantmentLevelEntry(entry.getKey(), entry.getValue()));
                    }
                    
                    // 创建新的交易（价格保持不变，只增强附魔书）
                    TradeOffer enhancedTrade = new TradeOffer(
                        trade.getOriginalFirstBuyItem(),
                        trade.getSecondBuyItem().orElse(ItemStack.EMPTY),
                        enhancedBook,
                        trade.getUses(),
                        trade.getMaxUses(),
                        trade.getMerchantExperience(),
                        trade.getPriceMultiplier(),
                        trade.getDemandBonus()
                    );
                    
                    // 替换原交易
                    trades.set(i, enhancedTrade);
                }
            }
        }
        
        // 添加额外的高价值附魔书交易（可选）
        addHighValueTrades(trades, random);
    }
    
    private void addHighValueTrades(TradeOfferList trades, Random random) {
        // 有一定几率添加特殊的高价值附魔书交易
        if (random.nextFloat() < 0.3f) { // 30% 几率
            ItemStack mendingBook = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantedBookItem.addEnchantment(mendingBook, 
                new net.minecraft.enchantment.EnchantmentLevelEntry(Enchantments.MENDING, 1));
            
            // 创建Mending附魔书交易（需要32-64绿宝石）
            int emeraldCost = 32 + random.nextInt(33); // 32-64之间
            ItemStack emeralds = new ItemStack(Items.EMERALD, emeraldCost);
            
            TradeOffer mendingTrade = new TradeOffer(
                emeralds,
                ItemStack.EMPTY,
                mendingBook,
                1, // 只能使用一次
                1,
                30, // 经验值
                0.05f, // 价格乘数
                0 // 需求奖励
            );
            
            trades.add(mendingTrade);
        }
    }
}