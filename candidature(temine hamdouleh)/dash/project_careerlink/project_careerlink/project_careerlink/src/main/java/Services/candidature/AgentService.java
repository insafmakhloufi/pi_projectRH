package Services.candidature;

import Entities.candidature.AnalyseResult;

import Entities.candidature.Candidature;
import Entities.candidature.Certification;
import Entities.candidature.Experience;

import java.util.List;

public class AgentService {

    private final CandidatureService candidatureService = new CandidatureService();
    private final AnalyseIAService analyseIAService = new AnalyseIAService();

    public AnalyseResult analyser(int idCandidat) throws Exception {
        Candidature c = candidatureService.getCandidatureById(idCandidat);
        if (c == null) {
            throw new IllegalArgumentException("Candidature introuvable: IDCandidat=" + idCandidat);
        }

        List<Experience> experiences = candidatureService.getExperiencesByCandidatureId(idCandidat);
        List<Certification> certifications = candidatureService.getCertificationsByCandidatureId(idCandidat);

        return analyseIAService.analyser(c, experiences, certifications);
    }
}
