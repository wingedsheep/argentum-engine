package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tidebinder Mage
 * {U}{U}
 * Creature — Merfolk Wizard
 * When this creature enters, tap target red or green creature an opponent controls. That creature
 * doesn't untap during its controller's untap step for as long as you control this creature.
 * 2/2
 */
val TidebinderMage = card("Tidebinder Mage") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, tap target red or green creature an opponent controls. " +
        "That creature doesn't untap during its controller's untap step for as long as you control this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls.withAnyColor(Color.RED, Color.GREEN))
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.WhileYouControlSource("Tidebinder Mage"),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "John Severin Brassell"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e032d1dd-6efc-4f6c-ad3b-30fe74845edf.jpg?1783939930"
        ruling(
            "2013-07-01",
            "If you lose control of Tidebinder Mage between when it entered the battlefield and when the " +
                "ability resolves, the target creature will become tapped. However, it won't be affected by " +
                "the other effect and will untap as normal during its controller's untap step.",
        )
        ruling(
            "2013-07-01",
            "If another player gains control of Tidebinder Mage, the creature will no longer be affected by " +
                "the ability preventing it from untapping, even if you later regain control of Tidebinder Mage.",
        )
    }
}
