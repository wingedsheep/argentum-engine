package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Geralf, the Fleshwright
 * {2}{U}
 * Legendary Creature — Human Warlock
 * 2/3
 *
 * Whenever you cast a spell during your turn other than your first spell that turn, create a 2/2
 * blue and black Zombie Rogue creature token.
 * Whenever a Zombie you control enters, put a +1/+1 counter on it for each other Zombie that
 * entered the battlefield under your control this turn.
 *
 * - First ability: "a spell during your turn other than your first spell that turn" → the spell-cast
 *   trigger [Triggers.YouCastSpell] gated by an intervening "if" that it is your turn and you have
 *   already cast a spell this turn. The triggering spell is itself counted in
 *   `spellsCastThisTurnByPlayer` at the time the cast trigger is checked (cf. Inventive Wingsmith /
 *   Outlaw Stitcher), so [Conditions.YouCastSpellsThisTurn](atLeast = 2) is true exactly for the
 *   2nd and every later spell. Spells cast before Geralf was on the battlefield count
 *   (2024-04-12 ruling); the gate just compares the running cast count.
 * - Second ability: "a Zombie you control enters" → an ETB trigger (ANY binding so it sees Geralf
 *   himself were he ever a Zombie) filtered to Zombies you control. The counter goes on the entering
 *   Zombie ([EffectTarget.TriggeringEntity]); the count is
 *   [DynamicAmounts.subtypeEnteredUnderControlThisTurn](Zombie, excludeTriggeringEntity = true) —
 *   "each *other* Zombie that entered the battlefield under your control this turn". This is a turn
 *   history count (it counts Zombies that have since left or changed type) and, per the 2024-04-12
 *   ruling, simultaneous Zombie entrants each see the others.
 */
val GeralfTheFleshwright = card("Geralf, the Fleshwright") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Warlock"
    power = 2
    toughness = 3
    oracleText = "Whenever you cast a spell during your turn other than your first spell that turn, " +
        "create a 2/2 blue and black Zombie Rogue creature token.\n" +
        "Whenever a Zombie you control enters, put a +1/+1 counter on it for each other Zombie that " +
        "entered the battlefield under your control this turn."

    triggeredAbility {
        trigger = Triggers.YouCastSpell
        triggerCondition = Conditions.All(
            Conditions.IsYourTurn,
            Conditions.YouCastSpellsThisTurn(atLeast = 2),
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE, Color.BLACK),
            creatureTypes = setOf("Zombie", "Rogue"),
            imageUri = "https://cards.scryfall.io/normal/front/7/4/74c7a0bd-6011-495a-b56c-8fa707dd7f12.jpg?1712316777",
        )
        description = "Whenever you cast a spell during your turn other than your first spell that " +
            "turn, create a 2/2 blue and black Zombie Rogue creature token."
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE).youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.AddDynamicCounters(
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.subtypeEnteredUnderControlThisTurn(
                subtype = Subtype.ZOMBIE,
                excludeTriggeringEntity = true,
            ),
            target = EffectTarget.TriggeringEntity,
        )
        description = "Whenever a Zombie you control enters, put a +1/+1 counter on it for each " +
            "other Zombie that entered the battlefield under your control this turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "50"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/afe3b678-b340-4c53-bbf6-19252a809d73.jpg?1712355427"

        ruling("2024-04-12", "Geralf's first ability will consider spells you cast before it was on the battlefield. For example, if Geralf is the first spell you cast during your turn, each subsequent spell you cast that turn after it's on the battlefield will cause its first ability to trigger.")
        ruling("2024-04-12", "If multiple Zombies enter the battlefield under your control at the same time, Geralf's last ability will trigger for each of those Zombies. Each of those triggered abilities will see each of those other Zombies that entered the battlefield plus any other Zombies that entered the battlefield under your control so far this turn.")
    }
}
