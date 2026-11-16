package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dragonlord Silumgar {4}{U}{B}
 * Legendary Creature — Elder Dragon
 * 3/5
 *
 * Flying, deathtouch
 * When Dragonlord Silumgar enters, gain control of target creature or planeswalker for as long as
 * you control Dragonlord Silumgar.
 *
 * The steal is Aladdin's [Duration.WhileYouControlSource]: losing Silumgar (it leaves, or another
 * player takes it) ends the control change for good, and if Silumgar is already gone or no longer
 * yours when the trigger resolves, the trigger does nothing (CR 611.2b; DTK rulings).
 */
val DragonlordSilumgar = card("Dragonlord Silumgar") {
    manaCost = "{4}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 3
    toughness = 5
    oracleText = "Flying, deathtouch\n" +
        "When Dragonlord Silumgar enters, gain control of target creature or planeswalker for as long " +
        "as you control Dragonlord Silumgar."

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val stolen = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker))
        effect = Effects.GainControl(stolen, Duration.WhileYouControlSource("Dragonlord Silumgar"))
        description = "When Dragonlord Silumgar enters, gain control of target creature or planeswalker " +
            "for as long as you control Dragonlord Silumgar."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "220"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2eae504d-d4a9-440f-8ceb-df967cc50494.jpg?1783938572"
    }
}
