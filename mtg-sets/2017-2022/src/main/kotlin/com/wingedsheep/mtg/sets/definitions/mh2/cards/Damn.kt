package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Damn — Modern Horizons 2 #80 (canonical printing)
 * {B}{B} · Sorcery
 *
 * Destroy target creature. A creature destroyed this way can't be regenerated.
 * Overload {2}{W}{W}
 *
 * Overloaded, the spell has no targets and destroys each creature, still without
 * regeneration — the Vandalblast shape with `noRegenerate` on both branches.
 */
val Damn = card("Damn") {
    manaCost = "{B}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. A creature destroyed this way can't be regenerated.\n" +
        "Overload {2}{W}{W} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{2}{W}{W}"))

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature, noRegenerate = true)

        overloadEffect = Effects.DestroyAll(GameObjectFilter.Creature, noRegenerate = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efeae088-9ac5-4d2f-a15c-d8675a471ac5.jpg?1783926863"
        ruling(
            "2024-01-12",
            "Because a spell with overload doesn't target when its overload cost is paid, it may affect " +
                "permanents with hexproof or with protection from the appropriate color."
        )
        ruling(
            "2024-01-12",
            "If you are instructed to cast a spell with overload \"without paying its mana cost,\" you can't " +
                "choose to pay its overload cost instead."
        )
    }
}
