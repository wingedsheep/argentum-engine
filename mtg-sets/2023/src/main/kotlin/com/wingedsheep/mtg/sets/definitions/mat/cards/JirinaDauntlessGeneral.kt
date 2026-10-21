package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jirina, Dauntless General — March of the Machine: The Aftermath #32
 * {W}{B} · Legendary Creature — Human Soldier · 2/2
 *
 * When Jirina enters, exile target player's graveyard.
 * Sacrifice Jirina: Humans you control gain hexproof and indestructible until end of turn.
 *
 * "Humans" is any Human permanent you control (a Kindred Human counts too). The affected set is
 * locked in as the ability resolves (`ForEachInGroup`), per the ruling below.
 */
val JirinaDauntlessGeneral = card("Jirina, Dauntless General") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "When Jirina enters, exile target player's graveyard.\n" +
        "Sacrifice Jirina: Humans you control gain hexproof and indestructible until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
            exile(graveyard, player.asPlayer)
        }
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN).youControl()),
            Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Cristi Balanescu"
        flavorText = "\"Our strength lies not in our walls, but in our people.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c0c3860-1dab-4897-8f36-f9be263bba03.jpg?1783916517"

        ruling(
            "2023-05-12",
            "The set of creatures affected by Jirina's last ability is determined as the ability resolves. Humans you begin to control later in the turn, non-Human creatures you control that become Human, and noncreature permanents that become Human creatures later in the turn won't gain hexproof and indestructible."
        )
    }
}
