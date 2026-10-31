package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bonecrusher Giant // Stomp
 * {2}{R}
 * Creature — Giant
 * 4/3
 * Whenever this creature becomes the target of a spell, this creature deals 2 damage to that
 * spell's controller.
 *
 * Adventure: Stomp — {1}{R}, Instant — Adventure
 * Damage can't be prevented this turn. Stomp deals 2 damage to any target.
 *
 * The becomes-target trigger is narrowed to spells (Goldspan Dragon's `spellsOnly`), and "that
 * spell's controller" is [Player.ControllerOfCause] — the same handle Thunderbreak Regent uses
 * for the other end of the targeting. The trigger fires for any spell, including its controller's
 * own (e.g. an Aura or pump spell), as printed.
 */
val BonecrusherGiant = card("Bonecrusher Giant") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    oracleText = "Whenever this creature becomes the target of a spell, this creature deals 2 damage " +
        "to that spell's controller."
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(spellsOnly = true)
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.ControllerOfCause))
        description = "Whenever this creature becomes the target of a spell, this creature deals " +
            "2 damage to that spell's controller."
    }

    adventure("Stomp") {
        manaCost = "{1}{R}"
        typeLine = "Instant — Adventure"
        oracleText = "Damage can't be prevented this turn. Stomp deals 2 damage to any target. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DamageCantBePreventedThisTurn() then Effects.DealDamage(2, t)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Victor Adame Minguez"
        flavorText = "Not every tale ends in glory."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09fd2d9c-1793-4beb-a3fb-7a869f660cd4.jpg?1783932628"
        ruling("2019-10-04", "Bonecrusher Giant's ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2019-10-04", "If a spell targets Bonecrusher Giant more than once, its ability triggers only once.")
        ruling("2019-10-04", "Stomp only stops damage from being prevented by effects that specifically use the word \"prevent.\"")
        ruling("2019-10-04", "If the chosen target is an illegal target by the time Stomp tries to resolve, the spell won't resolve. Damage can be prevented as usual.")
    }
}
