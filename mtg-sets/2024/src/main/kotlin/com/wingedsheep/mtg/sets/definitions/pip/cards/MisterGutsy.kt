package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mister Gutsy — Fallout #136
 * {2} · Artifact Creature — Robot Soldier · 1/1
 *
 * Whenever you cast an Aura or Equipment spell, put a +1/+1 counter on this creature.
 * When this creature dies, create X Junk tokens, where X is the number of +1/+1 counters on it.
 *
 * The dies trigger counts the +1/+1 counters from last-known information (Hangarback Walker's
 * shape): the creature is already in the graveyard when the trigger resolves.
 */
val MisterGutsy = card("Mister Gutsy") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Robot Soldier"
    oracleText = "Whenever you cast an Aura or Equipment spell, put a +1/+1 counter on this creature.\n" +
        "When this creature dies, create X Junk tokens, where X is the number of +1/+1 counters on it. " +
        "(They're artifacts with \"{T}, Sacrifice this token: Exile the top card of your library. " +
        "You may play that card this turn. Activate only as a sorcery.\")"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.Any.withSubtype(Subtype.AURA) or GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT)
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateJunk(DynamicAmounts.lastKnownPlusOneCounters())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f09166a-78de-4136-8a3c-034e1ef73ede.jpg?1783912378"
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
        ruling("2024-03-08", "You pay all costs and follow all normal timing rules for the card played from exile with a Junk token's ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2024-03-08", "Some spells and abilities that create Junk tokens may require targets. If each target chosen is illegal as that spell or ability tries to resolve, it won't resolve and none of its effects happen. You won't create any Junk tokens.")
    }
}
