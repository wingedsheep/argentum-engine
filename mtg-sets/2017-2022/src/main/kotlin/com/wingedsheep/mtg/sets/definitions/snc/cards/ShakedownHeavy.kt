package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shakedown Heavy
 * {2}{B}
 * Creature — Ogre Warrior
 * 6/4
 * Menace
 * Whenever this creature attacks, defending player may have you draw a card. If they do, untap
 * this creature and remove it from combat.
 *
 * The "may" belongs to the defending player, so it is routed to them through [Effects.May]'s
 * `decisionMaker` (the same shape as Palantír of Orthanc). Declining does nothing — Shakedown
 * Heavy keeps attacking.
 */
val ShakedownHeavy = card("Shakedown Heavy") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Warrior"
    oracleText = "Menace\nWhenever this creature attacks, defending player may have you draw a card. " +
        "If they do, untap this creature and remove it from combat."
    power = 6
    toughness = 4

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.May(
            effect = Effects.DrawCards(1, EffectTarget.Controller) then
                Effects.Untap(EffectTarget.Self) then
                Effects.RemoveFromCombat(EffectTarget.Self),
            prompt = "Have Shakedown Heavy's controller draw a card? If you do, it's removed from combat.",
            decisionMaker = EffectTarget.PlayerRef(Player.DefendingPlayer),
            descriptionOverride = "Defending player may have you draw a card. If they do, untap this creature and remove it from combat."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "95"
        artist = "Randy Vargas"
        flavorText = "\"Look, we both get what we want. I get your Halo. You get to keep your kneecaps.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce418184-a672-46bc-a17d-c559614defb5.jpg?1783923123"
    }
}
