package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.targeting.TargetCreature

/**
 * Nameless Inversion
 *
 * {1}{B} Kindred Instant — Shapeshifter
 * Changeling
 * Target creature gets +3/-3 and loses all creature types until end of turn.
 */
object NamelessInversion {
    val definition = CardDefinition.instant(
        name = "Nameless Inversion",
        manaCost = ManaCost.parse("{1}{B}"),
        // Note: Kindred Instant — Shapeshifter type line needs TypeLine enhancement
        keywords = setOf(Keyword.CHANGELING),
        oracleText = "Changeling\nTarget creature gets +3/-3 and loses all creature types until end of turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "113",
            rarity = Rarity.UNCOMMON,
            artist = "Dominik Mayer",
            imageUri = "https://cards.scryfall.io/normal/front/b/b/bb5b5b5b-5b5b-5b5b-5b5b-5b5b5b5b5b5b.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Nameless Inversion") {
        keywords(Keyword.CHANGELING)

        targets(TargetCreature())

        // +3/-3 and loses all creature types until end of turn
        // TODO: "loses all creature types" effect needs additional infrastructure
        spell(
            ModifyStatsEffect(
                powerModifier = 3,
                toughnessModifier = -3,
                target = EffectTarget.TargetCreature,
                untilEndOfTurn = true
            )
        )
    }
}
