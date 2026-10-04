package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Annex Sentry
 * {2}{W}
 * Artifact Creature — Phyrexian Cleric
 * 1/4
 *
 * Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)
 * When this creature enters, exile target artifact or creature an opponent controls with
 * mana value 3 or less until this creature leaves the battlefield.
 *
 * Modeled with ETB + LTB triggers linked through LinkedExileComponent.
 */
val AnnexSentry = card("Annex Sentry") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Phyrexian Cleric"
    power = 1
    toughness = 4
    oracleText = "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "When this creature enters, exile target artifact or creature an opponent controls with mana value 3 or less until this creature leaves the battlefield."

    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(
            TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).opponentControls().manaValueAtMost(3))
        )
        effect = Effects.ExileUntilLeaves(permanent)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "David Astruga"
        flavorText = "There's never a need to change the watch, for the servants of Norn are sleepless."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04baad61-1b51-4602-9e33-0de4a9f34793.jpg?1783918086"
    }
}
