package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Academic Probation — Strixhaven: School of Mages #7 (canonical printing)
 * {1}{W} · Sorcery — Lesson
 *
 * Choose one —
 * • Choose a nonland card name. Opponents can't cast spells with the chosen name until your next turn.
 * • Choose target nonland permanent. Until your next turn, it can't attack or block, and its
 *   activated abilities can't be activated.
 *
 * The first mode's name is frozen into the ban as it resolves ([Effects.CantCastSpells] rewrites
 * `namedFromVariable` into a literal name), so later casts are checked against it. The second mode
 * is Edifice of Authority's lock on a nonland permanent.
 */
val AcademicProbation = card("Academic Probation") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery — Lesson"
    oracleText = "Choose one —\n" +
        "• Choose a nonland card name. Opponents can't cast spells with the chosen name until your next turn.\n" +
        "• Choose target nonland permanent. Until your next turn, it can't attack or block, and its " +
        "activated abilities can't be activated."

    spell {
        modal(chooseCount = 1) {
            mode("Choose a nonland card name. Opponents can't cast spells with the chosen name until your next turn") {
                effect = Effects.Pipeline {
                    val chosenName = chooseCardName(
                        prompt = "Choose a nonland card name",
                        pool = CardNamePool.NONLAND
                    )
                    run(
                        Effects.CantCastSpells(
                            target = EffectTarget.PlayerRef(Player.EachOpponent),
                            duration = Duration.UntilYourNextTurn,
                            spellFilter = GameObjectFilter.Any.namedFromVariable(chosenName)
                        )
                    )
                }
            }
            mode("Choose target nonland permanent. Until your next turn, it can't attack or block, and its activated abilities can't be activated") {
                val permanent = target(TargetFilter.NonlandPermanent)
                effect = Effects.CantAttackOrBlock(permanent, Duration.UntilYourNextTurn) then
                    Effects.GrantStaticAbility(
                        ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                        target = permanent,
                        duration = Duration.UntilYourNextTurn
                    )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05521edf-f47f-4e7a-aec5-cdc4ae7368c2.jpg?1783927395"
        ruling("2021-04-16", "For the first mode, no one can cast spells between the time a card is named and the time that Academic Probation's effect starts to apply.")
        ruling("2021-04-16", "Academic Probation is a sorcery, but in some unusual cases it may be cast in response to other spells. Spells with the chosen name that somehow happen to already be on the stack when Academic Probation resolves will not be affected.")
        ruling("2021-04-16", "Although spells with the chosen name can't be cast, a card with that name can still be put onto the battlefield by another effect (if it's a permanent card).")
        ruling("2021-04-16", "You can name either face of a modal double-faced card, but not both, and you can name either half of a split card, but not both. Opponents may still cast the face or half you didn't name.")
        ruling("2021-04-16", "If you target a noncreature permanent with the second mode and it later becomes a creature, that creature still won't be able to attack or block until your next turn.")
        ruling("2021-04-16", "Activated abilities contain a colon. They're generally written \"[Cost]: [Effect].\" Some keyword abilities (such as equip) are activated abilities and will have a colon in their reminder text. Triggered abilities (starting with \"when,\" \"whenever,\" or \"at\") are unaffected.")
    }
}
