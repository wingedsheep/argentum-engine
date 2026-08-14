package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rot Farm Mortipede — Murders at Karlov Manor #102
 * {3}{B} · Creature — Insect · 3/4
 *
 * Whenever one or more creature cards leave your graveyard, this creature gets +1/+0 and gains
 * menace and lifelink until end of turn.
 *
 * A *batching* trigger ([Triggers.CardsLeaveYourGraveyard]): it fires at most once per batch no
 * matter how many creature cards left at once and no matter where they went — cast, exiled to pay
 * for collect evidence, reanimated, or returned to hand. Escaping three creature cards in one event
 * grows the Mortipede by +1/+0, not +3/+0.
 *
 * The three riders are one composite so they share a timestamp and expire together at cleanup
 * (CR 514.2). Each firing stacks, so two separate batches in a turn leave a 5/4 with menace and
 * lifelink — granting a keyword the creature already has is a harmless no-op.
 */
val RotFarmMortipede = card("Rot Farm Mortipede") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect"
    power = 3
    toughness = 4
    oracleText = "Whenever one or more creature cards leave your graveyard, this creature gets " +
        "+1/+0 and gains menace and lifelink until end of turn."

    triggeredAbility {
        trigger = Triggers.CardsLeaveYourGraveyard(GameObjectFilter.Creature)
        effect = Effects.Composite(
            Effects.ModifyStats(1, 0, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self),
        )
        description = "Whenever one or more creature cards leave your graveyard, this creature " +
            "gets +1/+0 and gains menace and lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Loïc Canavaggia"
        flavorText = "Every week, the necropsy lab's unclaimed bodies are transferred to the " +
            "Golgari for \"recycling.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/2/023b0142-663a-47e7-a9f1-0b565a172b60.jpg?1783912891"
    }
}
