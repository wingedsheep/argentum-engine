package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Lotusguard Disciple — Aetherdrift #21
 * {2}{W} · Creature — Bird Cleric · 2/2
 *
 * Flying
 * When this creature enters, target creature or Vehicle gains lifelink and indestructible until end of turn.
 *
 * Targets any [GameObjectFilter.CreatureOrVehicle] (a Vehicle matched by subtype). Both keywords
 * are granted via trigger-granted [Effects.GrantKeyword], which defaults to end-of-turn duration.
 */
val LotusguardDisciple = card("Lotusguard Disciple") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Cleric"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, target creature or Vehicle gains lifelink and indestructible until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "target creature or Vehicle",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))
        )
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.LIFELINK, t),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t)
        )
        description = "When this creature enters, target creature or Vehicle gains lifelink and " +
            "indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Josiah \"Jo\" Cameron"
        flavorText = "\"No one dies under my watch.\"\n—Sektem, Lotusguard veteran"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80645651-3804-481f-8f8f-ade762a011e1.jpg?1782687948"
    }
}
