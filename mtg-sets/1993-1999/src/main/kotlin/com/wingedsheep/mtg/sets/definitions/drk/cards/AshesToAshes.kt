package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/** Ashes to Ashes — exile two nonartifact creatures, then deal 5 damage to you. */
val AshesToAshes = card("Ashes to Ashes") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile two target nonartifact creatures. Ashes to Ashes deals 5 damage to you."

    spell {
        target = TargetCreature(count = 2, filter = TargetFilter.Creature.nonartifact())
        effect = Effects.Composite(
            ForEachTargetEffect(listOf(Effects.Exile(EffectTarget.ContextTarget(0)))),
            Effects.DealDamage(5, EffectTarget.PlayerRef(Player.You)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Drew Tucker"
        flavorText = "\"All rivers eventually run to the sea. My job is to sort out who goes first.\" —Maeveen O'Donagh, Memoirs of a Soldier"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/825496e5-19c7-4f50-8070-0265a58608dc.jpg?1783947941"
        ruling("2009-10-01", "If one targeted creature is illegal as Ashes to Ashes resolves, it still exiles the other one and deals 5 damage to you.")
        ruling("2009-10-01", "If both targeted creatures are illegal as Ashes to Ashes resolves, the spell is countered and doesn't deal 5 damage to you.")
    }
}
