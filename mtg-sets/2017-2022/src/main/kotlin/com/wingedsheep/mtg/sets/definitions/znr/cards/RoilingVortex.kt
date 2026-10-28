package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Roiling Vortex — Zendikar Rising #156.
 * {1}{R} Enchantment
 *
 * At the beginning of each player's upkeep, this enchantment deals 1 damage to them.
 * Whenever a player casts a spell, if no mana was spent to cast that spell, this enchantment
 * deals 5 damage to that player.
 * {R}: Your opponents can't gain life this turn.
 *
 * The free-cast clause is the `Conditions.TriggeringSpellCastWithoutPayingMana` intervening-if
 * (Vexing Bauble, Boromir) over the triggering spell's cast-mana record.
 */
val RoilingVortex = card("Roiling Vortex") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each player's upkeep, this enchantment deals 1 damage to them.\n" +
        "Whenever a player casts a spell, if no mana was spent to cast that spell, this enchantment deals 5 damage to that player.\n" +
        "{R}: Your opponents can't gain life this turn."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts()
        interveningIf = Conditions.TriggeringSpellCastWithoutPayingMana
        effect = Effects.DealDamage(5, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.LockLifeGain(EffectTarget.PlayerRef(Player.EachOpponent), Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b057eb7-8439-4d26-89df-c345ab2773e1.jpg?1783929353"
    }
}
