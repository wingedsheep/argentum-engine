package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Can't Stay Away
 * {W}{B}
 * Sorcery
 * Return target creature card with mana value 3 or less from your graveyard to the battlefield.
 * It gains "If this creature would die, exile it instead."
 * Flashback {3}{W}{B}
 *
 * The granted replacement is a permanent ExileOnDeath mark, applied only when the card actually
 * reached the battlefield; it ends when the creature leaves the battlefield (a new object).
 */
val CantStayAway = card("Can't Stay Away") {
    manaCost = "{W}{B}"
    colorIdentity = "BW"
    typeLine = "Sorcery"
    oracleText = "Return target creature card with mana value 3 or less from your graveyard to the battlefield. " +
        "It gains \"If this creature would die, exile it instead.\"\n" +
        "Flashback {3}{W}{B} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        target(TargetFilter.CreatureInYourGraveyard.manaValueAtMost(3))

        effect = Effects.Pipeline {
            val returned = moveTracked(
                gather(CardSource.ChosenTargets),
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You)
            )
            ifNotEmpty(returned) {
                run(Effects.MarkExileOnDeath(returned.asTarget, Duration.Permanent))
            }
        }
    }

    keywordAbility(KeywordAbility.flashback("{3}{W}{B}"))

    metadata {
        rarity = Rarity.RARE
        flavorText = "Death is no impediment to dinner."
        collectorNumber = "213"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3ccfebf1-add1-4f62-bffa-9023dea5b7c4.jpg"
    }
}
