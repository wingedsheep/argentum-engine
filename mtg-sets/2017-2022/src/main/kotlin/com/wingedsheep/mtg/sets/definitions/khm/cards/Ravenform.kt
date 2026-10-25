package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ravenform
 * {2}{U}
 * Sorcery
 * Exile target artifact or creature. Its controller creates a 1/1 blue Bird creature token with flying.
 * Foretell {U}
 *
 * "Its controller" is read after the exile: [EffectTarget.TargetController] falls back to the
 * exiled permanent's last-known controller, as Rapid Hybridization does after its destroy.
 */
val Ravenform = card("Ravenform") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Exile target artifact or creature. Its controller creates a 1/1 blue Bird creature " +
        "token with flying.\nForetell {U} (During your turn, you may pay {2} and exile this card from " +
        "your hand face down. Cast it on a later turn for its foretell cost.)"

    spell {
        val t = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature))
        effect = Effects.Exile(t) then Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            controller = EffectTarget.TargetController,
            imageUri = "https://cards.scryfall.io/normal/front/1/a/1a0cb299-057d-4c39-9d73-3a3512526e4a.jpg?1783928081"
        )
    }

    keywordAbility(KeywordAbility.foretell("{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c2cfcd7-b43a-4dad-a033-e661f55ceecd.jpg?1783928258"
    }
}
