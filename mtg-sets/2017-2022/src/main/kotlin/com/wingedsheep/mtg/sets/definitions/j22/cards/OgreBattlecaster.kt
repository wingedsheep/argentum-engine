package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ogre Battlecaster
 * {2}{R}
 * Creature — Ogre Shaman
 * 3/3
 *
 * First strike
 * Whenever this creature attacks, you may cast target instant or sorcery card from your graveyard
 * by paying {R}{R} in addition to its other costs. If that spell would be put into a graveyard,
 * exile it instead. When you cast that spell, this creature gets +X/+0 until end of turn, where X
 * is that spell's mana value.
 *
 * Notes:
 *  - The cast happens while the trigger resolves (ruling), paying the mana cost plus {R}{R} — the
 *    `additionalManaCost` of [Effects.CastFromCollection]. A caster who can't pay the total casts
 *    nothing.
 *  - "When you cast that spell" is a reflexive trigger (CR 603.12), gated on the cast actually
 *    happening via the collection the cast publishes to; it goes on the stack above the spell.
 */
val OgreBattlecaster = card("Ogre Battlecaster") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Ogre Shaman"
    power = 3
    toughness = 3
    oracleText = "First strike\n" +
        "Whenever this creature attacks, you may cast target instant or sorcery card from your " +
        "graveyard by paying {R}{R} in addition to its other costs. If that spell would be put into " +
        "a graveyard, exile it instead. When you cast that spell, this creature gets +X/+0 until end " +
        "of turn, where X is that spell's mana value."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.Pipeline {
            val card = gather(CardSource.ChosenTargets)
            run(Effects.May(
                Effects.IfYouDo(
                    action = Effects.CastFromCollection(
                        card,
                        insteadOfGraveyard = AfterResolveDestination.EXILE,
                        storeCastTo = "battlecasterCast",
                        additionalManaCost = "{R}{R}",
                    ),
                    then = Effects.ReflexiveTrigger(
                        action = Effects.Nothing,
                        optional = false,
                        reflexiveEffect = Effects.ModifyStats(
                            DynamicAmounts.manaValueOf("battlecasterCast"),
                            DynamicAmounts.fixed(0),
                            EffectTarget.Self,
                        ),
                        descriptionOverride = "This creature gets +X/+0 until end of turn, where X " +
                            "is that spell's mana value.",
                    ),
                    successCriterion = SuccessCriterion.CollectionNonEmpty("battlecasterCast"),
                ),
                descriptionOverride = "You may cast target instant or sorcery card from your graveyard " +
                    "by paying {R}{R} in addition to its other costs.",
            ))
        }
        description = "Whenever this creature attacks, you may cast target instant or sorcery card " +
            "from your graveyard by paying {R}{R} in addition to its other costs. If that spell would " +
            "be put into a graveyard, exile it instead. When you cast that spell, this creature gets " +
            "+X/+0 until end of turn, where X is that spell's mana value."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/298f1ab2-4c66-4d91-8f6a-1bad230632df.jpg?1783919183"
        ruling("2022-12-02", "You must cast the spell as Ogre Battlecaster's triggered ability resolves. You can't wait and cast it later.")
    }
}
