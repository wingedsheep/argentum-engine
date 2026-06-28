package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Summon: Bahamut
 * {9}
 * Enchantment Creature — Saga Dragon
 * 9/9
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I, II — Destroy up to one target nonland permanent.
 * III — Draw two cards.
 * IV — Mega Flare — This creature deals damage equal to the total mana value of other permanents
 *       you control to each opponent.
 * Flying
 */
val SummonBahamut = card("Summon: Bahamut") {
    manaCost = "{9}"
    typeLine = "Enchantment Creature — Saga Dragon"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I, II — Destroy up to one target nonland permanent.\n" +
        "III — Draw two cards.\n" +
        "IV — Mega Flare — This creature deals damage equal to the total mana value of other " +
        "permanents you control to each opponent.\n" +
        "Flying"
    power = 9
    toughness = 9

    keywords(Keyword.FLYING)

    // I, II — Destroy up to one target nonland permanent. ("up to one" → optional target.)
    sagaChapter(1) {
        val t = target("target", TargetObject(optional = true, filter = TargetFilter.NonlandPermanent))
        effect = Effects.Destroy(t)
    }
    sagaChapter(2) {
        val t = target("target", TargetObject(optional = true, filter = TargetFilter.NonlandPermanent))
        effect = Effects.Destroy(t)
    }

    // III — Draw two cards.
    sagaChapter(3) {
        effect = Effects.DrawCards(2)
    }

    // IV — Mega Flare — deal damage equal to the total mana value of other permanents you control
    //       to each opponent. excludeSelf drops Bahamut itself from the sum ("other permanents").
    sagaChapter(4) {
        effect = Effects.DealDamage(
            amount = DynamicAmount.AggregateBattlefield(
                player = Player.You,
                filter = GameObjectFilter.Any,
                aggregation = Aggregation.SUM,
                property = CardNumericProperty.MANA_VALUE,
                excludeSelf = true,
            ),
            target = EffectTarget.PlayerRef(Player.EachOpponent),
            damageSource = EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Arif Wijaya"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95318d85-4a08-47ac-a43d-ea83c0bea81c.jpg?1748705758"
    }
}
