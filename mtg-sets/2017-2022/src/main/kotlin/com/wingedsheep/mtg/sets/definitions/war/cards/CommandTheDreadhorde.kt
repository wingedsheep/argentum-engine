package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Command the Dreadhorde
 * {4}{B}{B}
 * Sorcery
 * Choose any number of target creature and/or planeswalker cards in graveyards. Command the
 * Dreadhorde deals damage to you equal to the total mana value of those cards. Put them onto the
 * battlefield under your control.
 *
 * The targets are gathered once (only the still-legal ones survive to resolution), the spell deals
 * damage to you equal to their summed mana value, and then the same cards move onto the
 * battlefield under your control. State-based actions aren't checked until the spell has finished
 * resolving, so the cards enter before a lethal hit makes you lose (ruling).
 */
val CommandTheDreadhorde = card("Command the Dreadhorde") {
    manaCost = "{4}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Choose any number of target creature and/or planeswalker cards in graveyards. " +
        "Command the Dreadhorde deals damage to you equal to the total mana value of those cards. " +
        "Put them onto the battlefield under your control."

    spell {
        targets(
            TargetFilter(GameObjectFilter.CreatureOrPlaneswalker, zone = Zone.GRAVEYARD),
            unlimited = true,
        )
        effect = Effects.Pipeline {
            val commanded = gather(CardSource.ChosenTargets)
            run(Effects.DealDamage(
                DynamicAmounts.manaValueSumOf(commanded),
                EffectTarget.PlayerRef(Player.You),
            ))
            move(commanded, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Daarken"
        flavorText = "Liliana seized the reins of Bolas's vast army, gritting her teeth behind the Chain Veil."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de7ec307-45e0-4853-85fe-9ce859b188c2.jpg?1783933449"
        ruling("2019-05-03", "Command the Dreadhorde deals damage to you before the creature and planeswalker cards are put onto the battlefield, but if this brings your life total to 0 or less, those creatures and planeswalkers enter the battlefield before you lose the game. Any abilities they have that trigger when they enter the battlefield won't resolve if you lose the game, but their static abilities (such as that of Platinum Angel) may help you survive with 0 or less life.")
        ruling("2019-05-03", "If a card in a graveyard has {X} in its mana cost, X is considered to be 0.")
    }
}
