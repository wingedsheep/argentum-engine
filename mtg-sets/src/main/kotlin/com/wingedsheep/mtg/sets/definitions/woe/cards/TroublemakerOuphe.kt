package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Troublemaker Ouphe
 * {1}{G}
 * Creature — Ouphe
 * 2/2
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * When this creature enters, if it was bargained, exile target artifact or enchantment an opponent
 * controls.
 *
 * The permanent shape of bargain (CR 702.166b): the bargained fact is stamped on the spell and
 * rides the permanent it becomes, so the enters trigger can still read it. Modelled as an
 * intervening-'if' clause (CR 603.4) on [Conditions.WasBargained] — when the Ouphe was cast without
 * bargaining, the ability never goes on the stack at all, so no target is ever chosen.
 *
 * Note the ordering this implies: a bargained cast sacrifices the artifact/enchantment as a cost
 * during casting, so that permanent is already gone and can never be the trigger's own target.
 */
val TroublemakerOuphe = card("Troublemaker Ouphe") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ouphe"
    power = 2
    toughness = 2
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "When this creature enters, if it was bargained, exile target artifact or enchantment an " +
        "opponent controls."

    bargain()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasBargained
        val permanent = target(
            "target artifact or enchantment an opponent controls",
            TargetPermanent(filter = TargetFilter.ArtifactOrEnchantment.opponentControls()),
        )
        effect = Effects.Exile(permanent)
        description = "When this creature enters, if it was bargained, exile target artifact or " +
            "enchantment an opponent controls."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Jesper Ejsing"
        flavorText = "Heavy is the head that wears the vase."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f7b2fc0-d3f6-4c4d-a163-986a372e5b12.jpg?1783915075"
    }
}
