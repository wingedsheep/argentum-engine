package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sorcerer Class {U}{R}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * When this Class enters, draw two cards, then discard two cards.
 *
 * {U}{R}: Level 2
 * Creatures you control have "{T}: Add {U} or {R}. Spend this mana only to cast an instant or
 * sorcery spell or to gain a Class level."
 *
 * {3}{U}{R}: Level 3
 * Whenever you cast an instant or sorcery spell, that spell deals damage to each opponent equal
 * to the number of instant and sorcery spells you've cast this turn.
 *
 * Level 2's mana is `AnyOf(InstantOrSorceryOnly, ClassLevelUpOnly)` — it pays any Class's
 * level-up (this one's level 3, or another Class's), but not the other activated abilities a
 * Class grants. Level 3's damage comes from the triggering spell ([EffectTarget.TriggeringEntity]
 * as the damage source, read with last-known information if it has left the stack), and the count
 * is taken as the trigger resolves, so spells cast in response count too (rulings).
 */
val SorcererClass = card("Sorcerer Class") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "When this Class enters, draw two cards, then discard two cards.\n" +
        "{U}{R}: Level 2 — Creatures you control have \"{T}: Add {U} or {R}. Spend this mana only to cast an instant or sorcery spell or to gain a Class level.\"\n" +
        "{3}{U}{R}: Level 3 — Whenever you cast an instant or sorcery spell, that spell deals damage to each opponent equal to the number of instant and sorcery spells you've cast this turn."

    // Level 1: when this Class enters, draw two cards, then discard two cards.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.loot(draw = 2, discard = 2)
    }

    // Level 2: creatures you control tap for {U} or {R}, spendable only on instants, sorceries
    // and Class levels.
    classLevel(2, "{U}{R}") {
        staticAbility {
            ability = GrantActivatedAbility(
                ability = ActivatedAbility(
                    cost = Costs.Tap,
                    effect = Effects.AddManaOfChoice(
                        colorSet = ManaColorSet.Specific(setOf(Color.BLUE, Color.RED)),
                        restriction = ManaRestriction.AnyOf(
                            listOf(ManaRestriction.InstantOrSorceryOnly, ManaRestriction.ClassLevelUpOnly)
                        ),
                    ),
                    timing = TimingRule.ManaAbility,
                    isManaAbility = true,
                ),
                filter = GroupFilter(GameObjectFilter.Creature.youControl())
            )
        }
    }

    // Level 3: each instant or sorcery you cast deals damage to each opponent.
    classLevel(3, "{3}{U}{R}") {
        triggeredAbility {
            trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
            effect = Effects.DealDamage(
                DynamicAmounts.spellsCastThisTurn(Player.You, GameObjectFilter.InstantOrSorcery),
                EffectTarget.PlayerRef(Player.EachOpponent),
                damageSource = EffectTarget.TriggeringEntity,
            )
            description = "Whenever you cast an instant or sorcery spell, that spell deals damage to each " +
                "opponent equal to the number of instant and sorcery spells you've cast this turn."
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f754b385-a28d-48de-a91f-2b4f33cc47f7.jpg?1783926444"
        ruling("2021-07-23", "The last ability counts the number of spells that were cast before it resolved, not the number of spells that were cast before it was placed on the stack. For example, if you cast a sorcery and then respond to Sorcerer Class's triggered ability by casting an instant, both triggered abilities will deal 2 damage when they resolve.")
        ruling("2021-07-23", "If the spell that causes the last ability to trigger is countered or is otherwise no longer on the stack as the ability resolves, it will still deal damage. Last known information will be used to do determine the effects of that damage. For example, if the spell has lifelink, you will still gain life when it deals damage, even if the spell was countered.")
        ruling("2021-07-23", "Copying a spell won't cause Sorcerer Class's level 3 ability to trigger and won't be counted toward the number of spells you've cast during the turn. However, if an effect copies a card and you cast the copy, that will count for both of those things.")
        ruling("2021-07-23", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2021-07-23", "You can multiclass or even control multiple Class enchantments of the same class. Each Class permanent tracks its own level separately.")
    }
}
