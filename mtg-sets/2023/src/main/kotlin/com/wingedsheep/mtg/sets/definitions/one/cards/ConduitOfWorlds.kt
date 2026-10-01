package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Conduit of Worlds
 * {2}{G}{G}
 * Artifact
 *
 * You may play lands from your graveyard.
 * {T}: Choose target nonland permanent card in your graveyard. If you haven't cast a spell this
 * turn, you may cast that card. If you do, you can't cast additional spells this turn. Activate
 * only as a sorcery.
 *
 * The cast happens during resolution, paying the card's normal costs. "Haven't cast a spell this
 * turn" is read at resolution and counts every spell this turn, Conduit itself included.
 */
val ConduitOfWorlds = card("Conduit of Worlds") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "You may play lands from your graveyard.\n" +
        "{T}: Choose target nonland permanent card in your graveyard. If you haven't cast a spell " +
        "this turn, you may cast that card. If you do, you can't cast additional spells this turn. " +
        "Activate only as a sorcery."

    staticAbility {
        ability = MayPlayLandsFromGraveyard
    }

    activatedAbility {
        target(TargetFilter(GameObjectFilter.NonlandPermanent.ownedByYou(), zone = Zone.GRAVEYARD))
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            run(
                Effects.If(
                    Conditions.Not(Conditions.YouCastSpellsThisTurn(1)),
                    Effects.May(
                        Effects.IfYouDo(
                            action = Effects.CastFromCollection(chosen, storeCastTo = "conduitCast"),
                            then = Effects.CantCastSpells(EffectTarget.Controller),
                            successCriterion = SuccessCriterion.CollectionNonEmpty("conduitCast"),
                        ),
                        descriptionOverride = "You may cast that card. If you do, you can't cast " +
                            "additional spells this turn.",
                    ),
                )
            )
        }
        description = "{T}: Choose target nonland permanent card in your graveyard. If you haven't " +
            "cast a spell this turn, you may cast that card. If you do, you can't cast additional " +
            "spells this turn. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Jokubas Uogintas"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/635146da-d415-4107-a7f9-2c46189e5c52.jpg?1783918018"

        ruling("2023-02-04", "The first ability of Conduit of Worlds doesn't change the times when you can play those land cards. You can still play only one land per turn, and only during your main phase when you have priority and the stack is empty.")
        ruling("2023-02-04", "The second ability of Conduit of Worlds allows you to cast the target card as the ability resolves. You can't wait and cast that card later in the turn. If you cast it, you can't cast any other spells this turn, even if another effect would allow you to.")
        ruling("2023-02-04", "The second ability of Conduit of Worlds will count spells you cast earlier in the turn even if Conduit of Worlds wasn't on the battlefield or under your control at that time. It will also count a spell you cast that subsequently was countered or failed to resolve. Notably, it will also include Conduit of Worlds itself on the turn you cast it.")
    }
}
