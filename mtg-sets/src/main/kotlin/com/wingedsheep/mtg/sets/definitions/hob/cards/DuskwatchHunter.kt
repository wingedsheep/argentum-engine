package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Duskwatch Hunter — The Hobbit #153
 * {2}{B/G} · Creature — Wolf · Common
 * 3/1
 *
 * This creature can't be blocked by tokens.
 * When this creature enters, put a +1/+1 counter on target creature.
 *
 * Modeling notes:
 *  - "Can't be blocked by tokens" narrows to creature tokens in practice — only creatures block —
 *    so the evasion is `CantBeBlockedBy(Creature.token())`.
 *  - The ETB counter targets *any* creature, including Duskwatch Hunter itself (making it a 4/2)
 *    and creatures an opponent controls; it is not optional, so a legal target must be chosen if
 *    one exists.
 */
val DuskwatchHunter = card("Duskwatch Hunter") {
    manaCost = "{2}{B/G}"
    colorIdentity = "BG"
    typeLine = "Creature — Wolf"
    power = 3
    toughness = 1
    oracleText = "This creature can't be blocked by tokens.\n" +
        "When this creature enters, put a +1/+1 counter on target creature."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.token())
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetCreature()
        effect = Effects.AddCounters("+1/+1", 1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Samuele Bandini"
        flavorText = "Packs of evil wolves lived under the shadow of the Goblin-infested mountains, " +
            "over the Edge of the Wild on the borders of the unknown."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3685c783-d837-4466-a960-ab3098db64c3.jpg?1785323286"
    }
}
