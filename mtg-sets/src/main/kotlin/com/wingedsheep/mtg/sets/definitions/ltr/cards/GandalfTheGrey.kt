package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gandalf the Grey
 * {3}{U}{R}
 * Legendary Creature — Avatar Wizard
 * 3/4
 *
 * Whenever you cast an instant or sorcery spell, choose one that hasn't been chosen —
 * • You may tap or untap target permanent.
 * • Gandalf deals 3 damage to each opponent.
 * • Copy target instant or sorcery spell you control. You may choose new targets for the copy.
 * • Put Gandalf on top of its owner's library.
 *
 * "Choose one that hasn't been chosen" is modeled with [ModalEffect.chooseOneNotYetChosen]:
 * the engine remembers which modes this Gandalf has already chosen (in a per-source
 * memory component) and never offers them again. Once all four have been chosen, the
 * ability has no legal mode and does nothing. The memory persists while this Gandalf
 * remains the same object; the fourth mode (return Gandalf to the library) resets it by
 * making Gandalf a new object on a later cast.
 */
val GandalfTheGrey = card("Gandalf the Grey") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Avatar Wizard"
    power = 3
    toughness = 4
    oracleText = "Whenever you cast an instant or sorcery spell, choose one that hasn't been chosen —\n" +
        "• You may tap or untap target permanent.\n" +
        "• Gandalf deals 3 damage to each opponent.\n" +
        "• Copy target instant or sorcery spell you control. You may choose new targets for the copy.\n" +
        "• Put Gandalf on top of its owner's library."

    triggeredAbility {
        trigger = Triggers.YouCastInstantOrSorcery
        effect = ModalEffect.chooseOneNotYetChosen(
            // • You may tap or untap target permanent.
            Mode.withTarget(
                MayEffect(
                    Effects.ChooseAction(
                        listOf(
                            EffectChoice("Tap it", Effects.Tap(EffectTarget.ContextTarget(0))),
                            EffectChoice("Untap it", Effects.Untap(EffectTarget.ContextTarget(0)))
                        )
                    ),
                    descriptionOverride = "You may tap or untap that permanent"
                ),
                Targets.Permanent,
                "You may tap or untap target permanent"
            ),
            // • Gandalf deals 3 damage to each opponent.
            Mode.noTarget(
                Effects.DealDamage(
                    3,
                    EffectTarget.PlayerRef(Player.EachOpponent),
                    damageSource = EffectTarget.Self
                ),
                "Gandalf deals 3 damage to each opponent"
            ),
            // • Copy target instant or sorcery spell you control. You may choose new targets for the copy.
            Mode.withTarget(
                Effects.CopyTargetSpell(),
                Targets.InstantOrSorcerySpellYouControl,
                "Copy target instant or sorcery spell you control. You may choose new targets for the copy"
            ),
            // • Put Gandalf on top of its owner's library.
            Mode.noTarget(
                Effects.PutOnTopOfLibrary(EffectTarget.Self),
                "Put Gandalf on top of its owner's library"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2b975e6-e709-481f-bfbc-41a832508283.jpg?1686969808"
    }
}
