package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Mirrorwing Dragon (EMN #136)
 * {3}{R}{R}
 * Creature — Dragon
 * 4/5
 *
 * Flying
 * Whenever a player casts an instant or sorcery spell that targets only this creature, that player
 * copies that spell for each other creature they control that the spell could target. Each copy
 * targets a different one of those creatures.
 *
 * Implementation notes:
 * - Both halves are engine vocabulary, so the card is plain data. The trigger is
 *   `anyPlayerCasts` — it watches every seat, not just this creature's controller — narrowed by
 *   [SpellCastPredicate.TargetsOnlySource], which is satisfied only when *every* instance of the word
 *   "target" on the spell points at this Dragon and nothing else ("targets only Mirrorwing Dragon and
 *   no other object or player", per the 2016-07-13 ruling).
 * - The payoff is [Effects.CopySpellForEachOtherPossibleTarget], the CR 707.10d "copy for each object
 *   it could target" shape. It resolves its candidate filter *and* control of the copies against the
 *   copied spell's controller, which is what makes the "they control" / "that player copies" wording
 *   work: cast Murder on an opponent's Mirrorwing Dragon and **your** creatures each get a Murder.
 *   Writing the filter as `Creature.youControl()` therefore reads "creature the caster controls".
 * - "Each copy targets a different one of those creatures" needs no extra vocabulary — 707.10d already
 *   means one copy per candidate with that candidate as its target, and unlike the 707.10c "you may
 *   choose new targets" family it involves no player decision at all.
 */
val MirrorwingDragon = card("Mirrorwing Dragon") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever a player casts an instant or sorcery spell that targets only this creature, that " +
        "player copies that spell for each other creature they control that the spell could target. " +
        "Each copy targets a different one of those creatures."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anyPlayerCasts(
            spellFilter = GameObjectFilter.InstantOrSorcery,
            requires = setOf(SpellCastPredicate.TargetsOnlySource)
        )
        effect = Effects.CopySpellForEachOtherPossibleTarget(
            candidates = GameObjectFilter.Creature.youControl()
        )
        description = "That player copies that spell for each other creature they control that the " +
            "spell could target. Each copy targets a different one of those creatures."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "136"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b7e33a8-765b-4909-a5c6-5fe8f8774a51.jpg?1783937458"

        ruling("2016-07-13", "The ability triggers whenever a player casts an instant or sorcery spell that targets only Mirrorwing Dragon and no other object or player.")
        ruling("2016-07-13", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copies will have the same mode. A different mode cannot be chosen.")
        ruling("2016-07-13", "The copies that the ability creates are created on the stack, so they're not cast. Abilities that trigger when a player casts a spell (like Mirrorwing Dragon's ability itself) won't trigger.")
        ruling("2016-07-13", "If a player casts an instant or sorcery spell that has multiple targets and Mirrorwing Dragon is chosen as the target in each instance, Mirrorwing Dragon's ability will trigger. Each of the copies will similarly be targeting only one of the player's other creatures.")
        ruling("2016-07-13", "The copies are only created targeting creatures that the spell's controller controls. Copies are not created for all creatures on the battlefield, and the affected creatures may be controlled by a different player than the controller of Mirrorwing Dragon. Notably, if you cast Murder targeting your opponent's Mirrorwing Dragon, your creatures will each get a Murder, not your opponent's.")
        ruling("2016-07-13", "If the spell that's copied has an X whose value was determined as it was cast (like Burn from Within does), the copies have the same value of X.")
        ruling("2016-07-13", "Any creature the player controls that couldn't be targeted by the original spell (due to shroud, protection abilities, targeting restrictions, or any other reason) is just ignored by Mirrorwing Dragon's ability. If the spell has multiple targets, a given creature must be a legal target for all of them or else a copy won't be created for that creature.")
        ruling("2016-07-13", "The player who cast the original spell controls all the copies. That player chooses the order the copies are put onto the stack. The original spell will be on the stack beneath those copies and will resolve last.")
    }
}
