package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kozilek, Butcher of Truth
 * {10}
 * Legendary Creature — Eldrazi
 * 12 / 12
 *
 * When you cast this spell, draw four cards.
 * Annihilator 4
 * When Kozilek is put into a graveyard from anywhere, its owner shuffles their graveyard into
 * their library.
 *
 * Modeling notes:
 *  - The draw is a cast trigger (`Triggers.self.isCast()`), so it resolves before Kozilek and
 *    even if Kozilek is countered.
 *  - Annihilator is a display-only [KeywordAbility.Numeric]; the attack trigger is lowered by hand
 *    as in Pathrazer of Ulamog: an edict the *defending player* answers.
 *  - The graveyard trigger fires from the graveyard (`triggerZone = GRAVEYARD`) on any zone
 *    change into it, and acts on the card's *owner* ([Player.OwnerOfSource]) — not whoever last
 *    controlled it — so a stolen Kozilek still shuffles its owner's graveyard. The whole
 *    graveyard is shuffled in, Kozilek included if it is still there.
 */
val KozilekButcherOfTruth = card("Kozilek, Butcher of Truth") {
    manaCost = "{10}"
    colorIdentity = ""
    typeLine = "Legendary Creature — Eldrazi"
    power = 12
    toughness = 12
    oracleText = "When you cast this spell, draw four cards.\n" +
        "Annihilator 4 (Whenever this creature attacks, defending player sacrifices four permanents of their choice.)\n" +
        "When Kozilek is put into a graveyard from anywhere, its owner shuffles their graveyard into their library."

    keywordAbility(KeywordAbility.annihilator(4))

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.DrawCards(4)
        description = "When you cast this spell, draw four cards."
    }

    // Annihilator 4 — the lowering of the display-only keyword ability above.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Sacrifice(
            GameObjectFilter.Permanent,
            4,
            EffectTarget.PlayerRef(Player.DefendingPlayer)
        )
        description = "Annihilator 4"
    }

    triggeredAbility {
        triggerZone = Zone.GRAVEYARD
        trigger = Triggers.self.changesZone(to = Zone.GRAVEYARD)
        effect = Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.PlayerRef(Player.OwnerOfSource))
        description = "When Kozilek is put into a graveyard from anywhere, its owner shuffles their graveyard into their library."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Michael Komarck"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/067fac91-2483-4678-b86a-2c54a3a480cf.jpg?1783942013"
        ruling("2018-12-07", "Kozilek's first ability triggers as you cast it, and that ability resolves before the spell itself. It resolves even if the spell is countered.")
        ruling("2010-06-15", "Annihilator abilities trigger and resolve during the declare attackers step. The defending player chooses and sacrifices the required number of permanents before they declare blockers. Any creatures sacrificed this way won't be able to block.")
        ruling("2010-06-15", "If a creature with annihilator is attacking a planeswalker, and the defending player chooses to sacrifice that planeswalker, the attacking creature continues to attack. It may be blocked. If it isn't blocked, it simply won't deal combat damage to anything.")
    }
}
