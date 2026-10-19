package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Unforgiving One — Kamigawa: Neon Dynasty #127 (canonical printing)
 * {2}{B} · Creature — Spirit · 2/3
 *
 * Menace. Whenever this creature attacks, return target creature card with mana value X or less
 * from your graveyard to the battlefield, where X is the number of modified creatures you control.
 *
 * The cap rides on the target filter as `manaValueAtMostDynamic` (Spellstutter Sprite's shape), so
 * X is read when the trigger picks its target *and* again on resolution — the 2022-02-18 ruling:
 * if the modified count drops enough in between, the ability doesn't resolve. "Modified" is
 * [StatePredicate.IsModified] (CR 700.9); Unforgiving One counts itself when it is modified.
 */
val UnforgivingOne = card("Unforgiving One") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever this creature attacks, return target creature card with mana value X or less from " +
        "your graveyard to the battlefield, where X is the number of modified creatures you control. " +
        "(Equipment, Auras you control, and counters are modifications.)"
    power = 2
    toughness = 3

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creatureCard = target(
            TargetFilter.CreatureInYourGraveyard.manaValueAtMostDynamic(
                DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsModified),
                ).count()
            ),
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(creatureCard)
        description = "Whenever this creature attacks, return target creature card with mana value X " +
            "or less from your graveyard to the battlefield, where X is the number of modified " +
            "creatures you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Fariba Khamseh"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cfb20c08-a84a-4773-a90c-fb007b94644b.jpg?1783923873"
        ruling("2022-02-18", "The value of X needs to be determined both when the ability triggers (so you can choose a target) and again when the ability resolves (to check if that target is still legal). If the number of modified creatures you control has decreased enough in that time to make the target illegal, Unforgiving One's ability won't resolve.")
        ruling("2022-02-18", "An Aura controlled by an opponent does not cause a creature you control to be modified.")
        ruling("2022-02-18", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
        ruling("2022-02-18", "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.")
    }
}
