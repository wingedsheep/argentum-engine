package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Aang, the Last Airbender — {3}{W} Legendary Creature — Human Avatar Ally — 3/2
 *
 * Flying
 * When Aang enters, airbend up to one other target nonland permanent. (Exile it. While it's
 * exiled, its owner may cast it for {2} rather than its mana cost.)
 * Whenever you cast a Lesson spell, Aang gains lifelink until end of turn.
 *
 * "up to one other target nonland permanent" = an optional, single [TargetPermanent] wrapped in
 * [TargetOther] (excludes Aang himself); [Effects.Airbend] airbends the chosen target. The Lesson
 * trigger grants lifelink to the source until end of turn (the grant defaults to EndOfTurn).
 */
val AangTheLastAirbender = card("Aang, the Last Airbender") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Avatar Ally"
    oracleText = "Flying\n" +
        "When Aang enters, airbend up to one other target nonland permanent. (Exile it. While it's exiled, its owner may cast it for {2} rather than its mana cost.)\n" +
        "Whenever you cast a Lesson spell, Aang gains lifelink until end of turn."
    power = 3
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target(
            "up to one other target nonland permanent",
            TargetOther(baseRequirement = TargetPermanent(count = 1, optional = true, filter = TargetFilter.NonlandPermanent))
        )
        effect = Effects.Airbend()
    }

    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withSubtype("Lesson"))
        effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Yueko"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/245e008c-e073-443f-9592-6f628c0026ec.jpg?1764119893"
    }
}
