package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ral, Storm Conduit — War of the Spark #211
 * {2}{U}{R} · Legendary Planeswalker — Ral · Loyalty 4
 *
 * Whenever you cast or copy an instant or sorcery spell, Ral deals 1 damage to target opponent or
 * planeswalker.
 * +2: Scry 1.
 * −2: When you next cast an instant or sorcery spell this turn, copy that spell. You may choose new
 *     targets for the copy.
 *
 * The static trigger is the magecraft shape (`castsOrCopies`), so the copy made by the −2 triggers
 * it a second time. The −2 is Galvanic Iteration's [Effects.CopyNextSpellCast] rider.
 */
val RalStormConduit = card("Ral, Storm Conduit") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Planeswalker — Ral"
    startingLoyalty = 4
    oracleText = "Whenever you cast or copy an instant or sorcery spell, Ral deals 1 damage to target " +
        "opponent or planeswalker.\n" +
        "+2: Scry 1.\n" +
        "−2: When you next cast an instant or sorcery spell this turn, copy that spell. You may choose " +
        "new targets for the copy."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        val victim = target(Targets.OpponentOrPlaneswalker)
        effect = Effects.DealDamage(1, victim)
        description = "Whenever you cast or copy an instant or sorcery spell, Ral deals 1 damage to " +
            "target opponent or planeswalker."
    }

    loyaltyAbility(+2) {
        effect = Patterns.Library.scry(1)
    }

    loyaltyAbility(-2) {
        effect = Effects.CopyNextSpellCast(1)
        description = "When you next cast an instant or sorcery spell this turn, copy that spell. " +
            "You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/678ce3bf-152d-45ca-87ae-ad46c921328b.jpg?1783933390"
        ruling("2019-05-03", "If an effect copies a spell multiple times, as Finale of Promise may, Ral's first ability triggers that many times.")
        ruling("2019-05-03", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copy created by Ral's last ability will have the same mode or modes. You can't choose different ones.")
        ruling("2019-05-03", "The copies that Ral's last ability creates are created on the stack, so they're not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
        ruling("2019-06-14", "If an effect copies a card rather than a spell (such as that of God-Eternal Kefnet), this doesn't cause Ral's first ability to trigger. That ability will trigger if you cast the copy, however.")
    }
}
