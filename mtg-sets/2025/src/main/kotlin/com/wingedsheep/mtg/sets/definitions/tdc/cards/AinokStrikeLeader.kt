package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ainok Strike Leader
 * {1}{W}
 * Creature — Dog Warrior
 * 2/2
 * Whenever you attack with this creature and/or your commander, for each opponent, create a 1/1 red
 * Goblin creature token that's tapped and attacking that player.
 * Sacrifice this creature: Creature tokens you control gain indestructible until end of turn.
 *
 * "Attack with this creature and/or your commander" is one `Triggers.you.attacks(with = …)` over
 * `sourceItself() or commander().ownedByYou()` — it fires once per attack declaration however many
 * of the two attack. "For each opponent … attacking that player" is Adeline's shape:
 * `ForEachPlayer(EachOpponent)` rebinds `Controller` to the visited opponent, so `attackedPlayer =
 * Controller` names that player while the token goes to the source's controller. The tokens attack
 * the player only, never a planeswalker they control.
 */
val AinokStrikeLeader = card("Ainok Strike Leader") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dog Warrior"
    power = 2
    toughness = 2
    oracleText = "Whenever you attack with this creature and/or your commander, for each opponent, create a " +
        "1/1 red Goblin creature token that's tapped and attacking that player.\n" +
        "Sacrifice this creature: Creature tokens you control gain indestructible until end of turn."

    triggeredAbility {
        trigger = Triggers.you.attacks(
            with = GameObjectFilter.Any.sourceItself() or GameObjectFilter.Any.commander().ownedByYou()
        )
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Goblin"),
                controller = EffectTarget.PlayerRef(Player.ControllerOfSource),
                tapped = true,
                attacking = true,
                attackedPlayer = EffectTarget.Controller,
                imageUri = "https://cards.scryfall.io/normal/front/e/2/e265ca24-96c0-4654-a8f3-bbffe288970a.jpg?1783906784"
            )
        )
        description = "Whenever you attack with this creature and/or your commander, for each opponent, " +
            "create a 1/1 red Goblin creature token that's tapped and attacking that player."
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Patterns.Group.grantKeywordToAll(
            Keyword.INDESTRUCTIBLE,
            GroupFilter(GameObjectFilter.Creature.token().youControl())
        )
        description = "Creature tokens you control gain indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Alexander Mokhov"
        flavorText = "When dalkovans need protecting, all respond."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cabd77a6-0913-4522-891d-c8a412a536c2.jpg?1783907186"
    }
}
