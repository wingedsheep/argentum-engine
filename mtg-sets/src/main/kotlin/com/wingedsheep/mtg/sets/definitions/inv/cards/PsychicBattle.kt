package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.RetargetChooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Psychic Battle
 * {3}{U}{U}
 * Enchantment
 * Whenever a player chooses one or more targets, each player reveals the top card of their
 * library. The player who reveals the card with the greatest mana value may change the target
 * or targets. If two or more cards are tied for greatest, the target or targets remain unchanged.
 * Changing targets this way doesn't trigger abilities of permanents named Psychic Battle.
 *
 * Invasion engine gap #19 — reveal-and-compare target swap, composed entirely from atoms. The
 * trigger fires on the new [com.wingedsheep.sdk.scripting.EventPattern.TargetsChosenEvent] (emitted
 * once per targeted spell or ability, with the spell/ability as the triggering entity). The effect
 * is a gather → compare → act pipeline:
 *  1. [GatherCardsEffect] over [CardSource.TopOfLibrary] for [Player.Each] (revealed) — each player
 *     reveals the top card of their library into the `revealed` collection.
 *  2. [FilterCollectionEffect] with [CollectionFilter.GreatestManaValue] — keep the greatest-mana-value
 *     card(s); a tie keeps several, so the next step finds no unique winner.
 *  3. [Effects.ChangeTriggeringObjectTargets] with [RetargetChooser.OwnerOfStored] — the owner of the
 *     single surviving card (none on a tie) may change the triggering object's targets.
 *
 * Because the targets are changed in place (not re-chosen via a put-on-stack), no fresh
 * `TargetsChosenEvent` is emitted — so Psychic Battle never re-triggers itself (matching the
 * printed errata clause).
 */
val PsychicBattle = card("Psychic Battle") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever a player chooses one or more targets, each player reveals the top card " +
        "of their library. The player who reveals the card with the greatest mana value may " +
        "change the target or targets. If two or more cards are tied for greatest, the target or " +
        "targets remain unchanged. Changing targets this way doesn't trigger abilities of " +
        "permanents named Psychic Battle."

    triggeredAbility {
        trigger = Triggers.AnyPlayerChoosesTargets
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(count = DynamicAmount.Fixed(1), player = Player.Each),
                storeAs = "revealed",
                revealed = true
            ),
            FilterCollectionEffect(
                from = "revealed",
                filter = CollectionFilter.GreatestManaValue,
                storeMatching = "greatestManaValue"
            ),
            Effects.ChangeTriggeringObjectTargets(
                chooser = RetargetChooser.OwnerOfStored("greatestManaValue")
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Ray Lago"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8758ca24-e613-43bf-be58-4cf557f82d0c.jpg?1562922363"
    }
}
