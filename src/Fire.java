import name.hon2a.asmp.xquery.Main;

public class Fire {
    public static void main(String[] args)
    {
        name.hon2a.asmp.xquery.Main xQuery = new Main();
        System.out.println(
                xQuery.run(
                        new String[] {
                                "C:\\Apps\\EasyPHP\\data\\localweb\\xmlcheck\\phptests\\plugins\\cases\\XQUERY\\PetrHudecekXQuery.zip"
                                ,
                                "5"
                        }
                )
        );
        /*
        name.hon2a.asmp.domsax.Main domSax = new Main();
        System.out.println(
                domSax.run(
                        new String[] {
                                "C:\\Apps\\EasyPHP\\data\\localweb\\xmlcheck\\phptests\\plugins\\cases\\DOMSAX\\domSax_correct.zip"
                        } ));
        */
    }
}

