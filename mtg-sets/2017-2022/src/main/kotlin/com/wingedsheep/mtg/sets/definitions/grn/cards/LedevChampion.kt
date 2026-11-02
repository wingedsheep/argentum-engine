package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ledev Champion — Guilds of Ravnica #186 (canonical printing)
 * {1}{G}{W} · Creature — Elf Knight · 2/2
 *
 * Whenever this creature attacks, you may tap any number of untapped creatures you control. This
 * creature gets +1/+1 until end of turn for each creature tapped this way.
 * {3}{G}{W}: Create a 1/1 white Soldier creature token with lifelink.
 *
 * Siege Striker's shape: the untapped creatures are gathered on resolution (so a vigilant Ledev
 * Champion may tap itself, per ruling), "any number" admits zero, and the bonus is the locked-in
 * count of creatures tapped.
 */
val LedevChampion = card("Ledev Champion") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Elf Knight"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature attacks, you may tap any number of untapped creatures you control. " +
        "This creature gets +1/+1 until end of turn for each creature tapped this way.\n" +
        "{3}{G}{W}: Create a 1/1 white Soldier creature token with lifelink."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = GameObjectFilter.Creature.youControl().untapped()
                )
            )
            val tapped = chooseAnyNumber(
                from = candidates,
                prompt = "You may tap any number of untapped creatures you control",
                useTargetingUI = true
            )
            run(Effects.TapCollection(tapped, tap = true))
            run(Effects.ModifyStats(
                power = tapped.count,
                toughness = tapped.count,
                target = EffectTarget.Self
            ))
        }
        description = "Whenever this creature attacks, you may tap any number of untapped creatures " +
            "you control. This creature gets +1/+1 until end of turn for each creature tapped this way."
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}{W}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            keywords = setOf(Keyword.LIFELINK)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc4f508b-a48d-4462-9d89-ee2ead3a0847.jpg?1783934128"
        ruling(
            "2018-10-05",
            "For Ledev Champion's first ability, the creatures to tap are chosen as the ability resolves. " +
                "No player may take actions between the time you choose the creatures to tap and the time " +
                "Ledev Champion's power and toughness are boosted."
        )
        ruling(
            "2018-10-05",
            "Attacking creatures with vigilance may be tapped for Ledev Champion's first ability. If Ledev " +
                "Champion itself has vigilance, it can be tapped this way, too. These creatures aren't " +
                "removed from combat."
        )
        ruling(
            "2018-10-05",
            "Creatures that have come under your control this turn can be tapped for Ledev Champion's " +
                "first ability."
        )
        ruling(
            "2018-10-05",
            "You can tap creatures this way only as Ledev Champion's triggered ability is resolving. You " +
                "can't tap more creatures later in combat to give it +1/+1 again."
        )
    }
}
